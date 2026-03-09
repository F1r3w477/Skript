package ch.njol.skript.fabric.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;

/**
 * Condition "%-object% exists" - true when the expression yields a non-null, non-blank value.
 */
public final class CondFabricExists implements Condition {

    private final Expression<Object> expr;

    public CondFabricExists(Expression<Object> expr) {
        this.expr = expr;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        Object val = expr != null ? expr.get(ctx) : null;
        if (val == null) return false;
        if (val instanceof CharSequence s) return s.toString().trim().length() > 0;
        return true;
    }
}
