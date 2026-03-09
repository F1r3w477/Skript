package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;

import java.util.Objects;

/**
 * Condition: %object% is not %object%. Negated equality.
 */
public final class CondCompareNot implements Condition {

    private final Expression<Object> leftExpr;
    private final Expression<Object> rightExpr;

    public CondCompareNot(Expression<Object> leftExpr, Expression<Object> rightExpr) {
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        Object a = leftExpr != null ? leftExpr.get(ctx) : null;
        Object b = rightExpr != null ? rightExpr.get(ctx) : null;
        return !Objects.equals(a, b);
    }
}
