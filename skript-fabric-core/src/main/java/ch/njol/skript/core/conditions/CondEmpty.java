package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.event.EventValues;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;

import java.util.Collection;

/**
 * Condition: %object% is empty / %object% is not empty.
 */
public final class CondEmpty implements Condition {

    private final Expression<Object> expr;
    private final boolean negated;

    public CondEmpty(Expression<Object> expr, boolean negated) {
        this.expr = expr;
        this.negated = negated;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        Object val = expr != null ? expr.get(ctx) : null;
        val = EventValues.resolve(val, ctx);
        boolean empty = isEmpty(val);
        return negated ? !empty : empty;
    }

    private static boolean isEmpty(Object o) {
        if (o == null) return true;
        if (o instanceof Collection<?> c) return c.isEmpty();
        if (o instanceof Object[] a) return a.length == 0;
        if (o instanceof CharSequence s) return s.length() == 0;
        if (o instanceof String s) return s.isEmpty();
        return false;
    }
}
