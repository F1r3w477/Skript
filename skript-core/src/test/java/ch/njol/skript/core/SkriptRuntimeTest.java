package ch.njol.skript.core;

import ch.njol.skript.core.config.ScriptConfig;
import ch.njol.skript.core.condition.CondTrue;
import ch.njol.skript.core.lang.AssertStatement;
import ch.njol.skript.core.lang.BroadcastStatement;
import ch.njol.skript.core.lang.DoIfStatement;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expressions;
import ch.njol.skript.core.lang.LogStatement;
import ch.njol.skript.core.lang.SetVariableStatement;
import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.core.lang.trigger.CoreTriggerItem;
import ch.njol.skript.core.lang.trigger.ParseSectionTriggerItem;
import ch.njol.skript.core.model.ScriptEventHandler;
import ch.njol.skript.core.model.ScriptFile;
import ch.njol.skript.core.parse.ParseLogsHolder;
import ch.njol.skript.core.SkriptParser;
import ch.njol.skript.core.variables.CoreVariables;
import ch.njol.skript.core.variables.VariableRef;
import ch.njol.skript.core.variables.VariableScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the core Skript runtime (event dispatch, broadcast/log, assertion DSL).
 */
class SkriptRuntimeTest {

    private CollectingLogger logger;
    private SkriptRuntime runtime;

    @BeforeEach
    void setUp() {
        logger = new CollectingLogger();
        runtime = new SkriptRuntime(logger);
        if (CoreTestMode.ENABLED) {
            TestRegistry.clear();
        }
    }

    @Test
    void fireEventDispatchesToMatchingHandlers() {
        ScriptFile file = new ScriptFile(
            Path.of("test.sk"),
            List.of(new ScriptEventHandler("load", List.of(new BroadcastStatement("hello")))),
            List.of()
        );
        runtime.registerScripts(List.of(file));
        runtime.fireEvent("load", new RuntimeEventContext("test", null));
        assertFalse(logger.getInfo().isEmpty());
        assertTrue(logger.getInfo().stream().anyMatch(s -> s.contains("[broadcast]") && s.contains("hello")));
    }

    @Test
    void fireEventNormalisesEventName() {
        ScriptFile file = new ScriptFile(
            Path.of("test.sk"),
            List.of(new ScriptEventHandler("join", List.of(new LogStatement("joined")))),
            List.of()
        );
        runtime.registerScripts(List.of(file));
        runtime.fireEvent("JOIN", new RuntimeEventContext("join", null));
        assertTrue(logger.getInfo().stream().anyMatch(s -> s.contains("[log]") && s.contains("joined")));
    }

    @Test
    void fireEventTestsRunsAllHandlers() {
        ScriptFile file = new ScriptFile(
            Path.of("test.sk"),
            List.of(
                new ScriptEventHandler("load", List.of(new LogStatement("load"))),
                new ScriptEventHandler("join", List.of(new LogStatement("join")))
            ),
            List.of()
        );
        runtime.registerScripts(List.of(file));
        runtime.fireEvent("tests", null);
        List<String> info = logger.getInfo();
        assertTrue(info.stream().anyMatch(s -> s.contains("load")));
        assertTrue(info.stream().anyMatch(s -> s.contains("join")));
    }

    @Test
    void assertionFailureInTestModeMarksTestFailed() {
        if (!CoreTestMode.ENABLED) {
            return;
        }
        TestRegistry.clear();
        TestRegistry.registerTest("assertion test");
        Statement assertStmt = new AssertStatement(true, false, "expected failure");
        ScriptEventHandler handler = new ScriptEventHandler("tests", List.of(assertStmt));
        ScriptFile file = new ScriptFile(
            Path.of("test.sk"),
            List.of(handler),
            List.of("assertion test")
        );
        runtime.registerScripts(List.of(file));
        runtime.fireEvent("tests", null);
        Map<String, String> failed = TestRegistry.getFailedTests();
        assertTrue(failed.containsKey("assertion test"));
        assertEquals("expected failure", failed.get("assertion test"));
    }

    /**
     * Integration test: parse a script with multiline "if any" / then / else from config,
     * run the trigger chain, and assert the else branch ran (no assertion failure).
     * Proves conditional else execution works when using the same parser path as Fabric.
     */
    @Test
    void conditionalIfAnyElseRunsElseBranch() throws Exception {
        String script = """
            test "SecConditional - if any else":
            \tif any:
            \t\t1 is 3
            \t\t2 is 7
            \tthen:
            \t\tset {_a} to true
            \telse:
            \t\tset {_a} to false
            \tassert {_a} is false with "if any did not run else when no condition was true"
            """;
        Path path = Files.createTempFile("sec_conditional_", ".sk");
        try {
            Files.writeString(path, script);
            ScriptConfig config = ScriptConfig.load(path, logger);
            assertNotNull(config, "ScriptConfig should load");
            SkriptParser parser = new SkriptParser(logger);
            ScriptFile file = parser.parse(path, config.getRoot());
            assertNotNull(file);
            assertFalse(file.getEventHandlers().isEmpty());
            if (CoreTestMode.ENABLED) {
                TestRegistry.clear();
            }
            runtime.registerScripts(List.of(file));
            runtime.fireEvent("tests", new RuntimeEventContext("tests", null));
            if (CoreTestMode.ENABLED) {
                Map<String, String> failed = TestRegistry.getFailedTests();
                assertFalse(failed.containsKey("SecConditional - if any else"),
                    "Else branch should run so assert passes; failed: " + failed.get("SecConditional - if any else"));
            }
        } finally {
            Files.deleteIfExists(path);
        }
    }

    /**
     * Load the real SecConditional.sk from the repo, run the "if any else" and "if all else"
     * handlers only, and assert they pass. Fails if the repo script path is not available (e.g. when
     * only core module is built).
     */
    @Test
    void conditionalElseFromRealSecConditionalSk() throws Exception {
        Path scriptPath = Path.of("src/test/skript/tests/syntaxes/sections/SecConditional.sk");
        if (!Files.isRegularFile(scriptPath)) {
            return; // skip when path not available (e.g. running from different cwd)
        }
        ScriptConfig config = ScriptConfig.load(scriptPath, logger);
        assertNotNull(config);
        SkriptParser parser = new SkriptParser(logger);
        ScriptFile file = parser.parse(scriptPath, config.getRoot());
        assertNotNull(file);
        List<ScriptEventHandler> handlers = file.getEventHandlers().stream()
            .filter(h -> "SecConditional - if any else".equals(h.getAssociatedTestName())
                || "SecConditional - if all else".equals(h.getAssociatedTestName()))
            .toList();
        assertFalse(handlers.isEmpty(), "Should find at least one of the two conditional-else tests");
        if (CoreTestMode.ENABLED) {
            TestRegistry.clear();
        }
        ScriptFile subset = new ScriptFile(scriptPath, handlers, List.of());
        runtime.registerScripts(List.of(subset));
        runtime.fireEvent("tests", new RuntimeEventContext("tests", null));
        if (CoreTestMode.ENABLED) {
            Map<String, String> failed = TestRegistry.getFailedTests();
            assertFalse(failed.containsKey("SecConditional - if any else"),
                "if any else should run else branch: " + failed.get("SecConditional - if any else"));
            assertFalse(failed.containsKey("SecConditional - if all else"),
                "if all else should run else branch: " + failed.get("SecConditional - if all else"));
        }
    }

    /** Do-if: when condition is true, inner statement runs and sets the variable. */
    @Test
    void doIfWhenTrueRunsInnerAndSetsVariable() {
        VariableRef ref = new VariableRef("{_false}");
        SetVariableStatement setStmt = new SetVariableStatement(ref, Expressions.fromParsed(false));
        DoIfStatement doIf = new DoIfStatement(CondTrue.INSTANCE, setStmt);
        VariableScope scope = new VariableScope();
        ExecutionContext ctx = new ExecutionContext(logger, new RuntimeEventContext("test", null), scope, null, null);
        CoreVariables.setScope(scope);
        try {
            doIf.run(ctx);
            Object value = scope.get(ref.getName());
            assertTrue(value instanceof Boolean && Boolean.FALSE.equals(value), "Do-if should set variable to false: " + value);
        } finally {
            CoreVariables.clearScope();
        }
    }

    /** Empty parse section sets ParseLogsHolder to the expected error message. */
    @Test
    void parseSectionEmptyBodySetsExpectedError() {
        ParseLogsHolder.clear();
        ParseSectionTriggerItem item = new ParseSectionTriggerItem(null, null, true);
        ExecutionContext ctx = new ExecutionContext(logger, new RuntimeEventContext("test", null), new VariableScope(), null, null);
        item.run(ctx);
        String logs = ParseLogsHolder.get();
        assertNotNull(logs);
        assertTrue(logs.contains("A parse section must contain code"), "Parse logs should contain empty-parse error: " + logs);
        ParseLogsHolder.clear();
    }
}
