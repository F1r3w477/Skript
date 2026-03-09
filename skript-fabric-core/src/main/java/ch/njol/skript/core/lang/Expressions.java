package ch.njol.skript.core.lang;

import ch.njol.skript.core.event.EventValue;
import ch.njol.skript.core.variables.VariableRef;

/**
 * Builds {@link Expression} instances from pattern-parse results. Use in syntax
 * factories so conditions and effects receive expressions instead of raw objects.
 */
public final class Expressions {

    private Expressions() {}

    /**
     * Wrap a parsed value as an expression. If it is an event value or variable ref,
     * it will be resolved at runtime; otherwise it is treated as a literal.
     *
     * @param parsed value from {@link ch.njol.skript.core.patterns.CoreSkriptPattern.CoreMatchResult#getExpression(int)}
     * @return expression that yields the resolved value in the current context
     */
    public static Expression<Object> fromParsed(Object parsed) {
        if (parsed == null) {
            return new LiteralExpression<>(null);
        }
        if (parsed instanceof EventValue || parsed instanceof VariableRef) {
            return new ResolvedExpression(parsed);
        }
        return new LiteralExpression<>(parsed);
    }
}
