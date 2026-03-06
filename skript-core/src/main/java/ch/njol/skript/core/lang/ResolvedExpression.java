package ch.njol.skript.core.lang;

import ch.njol.skript.core.event.EventValues;

/**
 * Expression that resolves a parsed value (literal, {@link ch.njol.skript.core.event.EventValue},
 * or {@link ch.njol.skript.core.variables.VariableRef}) at runtime via {@link EventValues#resolve}.
 */
public final class ResolvedExpression implements Expression<Object> {

    private final Object parsed;

    public ResolvedExpression(Object parsed) {
        this.parsed = parsed;
    }

    @Override
    public Object get(ExecutionContext ctx) {
        return EventValues.resolve(parsed, ctx);
    }
}
