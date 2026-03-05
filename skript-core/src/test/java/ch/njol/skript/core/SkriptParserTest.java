package ch.njol.skript.core;

import ch.njol.skript.core.lang.BroadcastStatement;
import ch.njol.skript.core.lang.LogStatement;
import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.core.model.ScriptEventHandler;
import ch.njol.skript.core.model.ScriptFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the core Skript parser (on ... / test "..." structures).
 */
class SkriptParserTest {

    private CollectingLogger logger;
    private SkriptParser parser;

    @BeforeEach
    void setUp() {
        logger = new CollectingLogger();
        parser = new SkriptParser(logger);
        if (CoreTestMode.ENABLED) {
            TestRegistry.clear();
        }
    }

    @Test
    void parsesSingleEventHandler() {
        List<String> lines = List.of(
            "on load:",
            "    broadcast \"hello\"",
            "    log \"world\""
        );
        ScriptFile file = parser.parse(Path.of("test.sk"), lines);
        assertNotNull(file);
        assertEquals(1, file.getEventHandlers().size());
        ScriptEventHandler handler = file.getEventHandlers().get(0);
        assertEquals("load", handler.getEventName());
        List<Statement> statements = handler.getStatements();
        assertEquals(2, statements.size());
        assertTrue(statements.get(0) instanceof BroadcastStatement);
        assertTrue(statements.get(1) instanceof LogStatement);
    }

    @Test
    void parsesMultipleHandlersAndNormalisesEventName() {
        List<String> lines = List.of(
            "on load:",
            "    log \"a\"",
            "on JOIN:",
            "    log \"b\"",
            "on server_started:",
            "    log \"c\""
        );
        ScriptFile file = parser.parse(Path.of("test.sk"), lines);
        assertNotNull(file);
        assertEquals(3, file.getEventHandlers().size());
        assertEquals("load", file.getEventHandlers().get(0).getEventName());
        assertEquals("join", file.getEventHandlers().get(1).getEventName());
        assertEquals("server_started", file.getEventHandlers().get(2).getEventName());
    }

    @Test
    void registersTestDeclarations() {
        List<String> lines = List.of(
            "test \"my test name\":",
            "    assert true is false with \"expected fail\"",
            "on tests:",
            "    log \"ok\""
        );
        if (CoreTestMode.ENABLED) {
            TestRegistry.clear();
        }
        ScriptFile file = parser.parse(Path.of("test.sk"), lines);
        assertNotNull(file);
        assertEquals(1, file.getEventHandlers().size());
        assertEquals(1, file.getTestNames().size());
        assertEquals("my test name", file.getTestNames().get(0));
    }

    @Test
    void emptyLinesAndCommentsInBody() {
        List<String> lines = List.of(
            "on load:",
            "",
            "    # comment",
            "    broadcast \"x\"",
            "    "
        );
        ScriptFile file = parser.parse(Path.of("test.sk"), lines);
        assertNotNull(file);
        assertEquals(1, file.getEventHandlers().size());
        // Empty lines and # comments are skipped; only broadcast line becomes a statement
        List<Statement> statements = file.getEventHandlers().get(0).getStatements();
        assertEquals(1, statements.size());
        assertTrue(statements.get(0) instanceof BroadcastStatement);
    }
}
