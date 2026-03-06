package ch.njol.skript.core.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the expression layer (Phase 7).
 */
class ExpressionTest {

    @Test
    void literalExpressionReturnsValue() {
        Expression<String> expr = new LiteralExpression<>("hello");
        assertNotNull(expr.get(null));
        assertEquals("hello", expr.get(null));
    }

    @Test
    void literalExpressionNull() {
        Expression<Object> expr = new LiteralExpression<>(null);
        assertNull(expr.get(null));
    }

    @Test
    void fromParsedLiteralWrapsAsLiteral() {
        Expression<Object> expr = Expressions.fromParsed("foo");
        assertNotNull(expr);
        assertEquals("foo", expr.get(null));
    }

    @Test
    void fromParsedNull() {
        Expression<Object> expr = Expressions.fromParsed(null);
        assertNotNull(expr);
        assertNull(expr.get(null));
    }
}
