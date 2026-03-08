package ch.njol.skript.core.syntax;

import ch.njol.skript.core.condition.CondFalse;
import ch.njol.skript.core.condition.CondTrue;
import ch.njol.skript.core.conditions.CondCompare;
import ch.njol.skript.core.conditions.CondContains;
import ch.njol.skript.core.conditions.CondIsSet;
import ch.njol.skript.core.lang.BroadcastStatement;
import ch.njol.skript.core.lang.DoIfStatement;
import ch.njol.skript.core.lang.LogStatement;
import ch.njol.skript.core.lang.SetVariableStatement;
import ch.njol.skript.core.lang.Statement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void parseSetVariableEffect() {
        SyntaxRegistry reg = SyntaxRegistry.get();
        Statement st = reg.parseEffect("set {_x} to 5");
        assertNotNull(st);
        assertInstanceOf(SetVariableStatement.class, st);
    }

    @Test
    void parseConditionsIsSetContainsCompare() {
        SyntaxRegistry reg = SyntaxRegistry.get();
        assertInstanceOf(CondIsSet.class, reg.parseCondition("{_x} is set"));
        assertInstanceOf(CondContains.class, reg.parseCondition("\"hello\" contains \"ell\""));
        // "1 is 1" / "1 is 2" are literals -> CondTrue/CondFalse; use "2 is 3" for CondCompare
        assertInstanceOf(CondCompare.class, reg.parseCondition("2 is 3"));
    }

    @Test
    void parseDoIfStatement() {
        SyntaxRegistry reg = SyntaxRegistry.get();
        Statement st = reg.parseStatement("set {_false} to false if 1 is 1");
        assertNotNull(st);
        assertInstanceOf(DoIfStatement.class, st);
    }
}
