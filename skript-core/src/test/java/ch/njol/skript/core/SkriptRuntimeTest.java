package ch.njol.skript.core;

import ch.njol.skript.core.lang.AssertStatement;
import ch.njol.skript.core.lang.BroadcastStatement;
import ch.njol.skript.core.lang.LogStatement;
import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.core.model.ScriptEventHandler;
import ch.njol.skript.core.model.ScriptFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
