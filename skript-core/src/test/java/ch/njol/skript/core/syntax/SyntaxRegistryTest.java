package ch.njol.skript.core.syntax;

import ch.njol.skript.core.condition.CondFalse;
import ch.njol.skript.core.condition.CondTrue;
import ch.njol.skript.core.lang.BroadcastStatement;
import ch.njol.skript.core.lang.LogStatement;
import ch.njol.skript.core.lang.Statement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for pattern-based syntax registry (Phase 3).
 */
class SyntaxRegistryTest {

    @Test
    void parseConditionTrueFalse() {
        SyntaxRegistry reg = SyntaxRegistry.get();
        assertNotNull(reg.parseCondition("true"));
        assertInstanceOf(CondTrue.class, reg.parseCondition("true"));
        assertInstanceOf(CondFalse.class, reg.parseCondition("false"));
        assertNull(reg.parseCondition("unknown"));
    }

    @Test
    void parseEffectBroadcastAndLog() {
        SyntaxRegistry reg = SyntaxRegistry.get();
        Statement st = reg.parseEffect("broadcast \"hello\"");
        assertNotNull(st);
        assertInstanceOf(BroadcastStatement.class, st);

        st = reg.parseEffect("log \"world\"");
        assertNotNull(st);
        assertInstanceOf(LogStatement.class, st);
    }

    @Test
    void parseStatementViaPattern() {
        SyntaxRegistry reg = SyntaxRegistry.get();
        Statement st = reg.parseStatement("broadcast \"pattern\"");
        assertNotNull(st);
        assertInstanceOf(BroadcastStatement.class, st);
    }
}
