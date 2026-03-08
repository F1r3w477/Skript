package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;

import java.util.Objects;

/**
 * Condition: %object% is/equals %object%. Uses the expression layer for runtime evaluation.
 */
public final class CondCompare implements Condition {

    private final Expression<Object> leftExpr;
    private final Expression<Object> rightExpr;

    public CondCompare(Expression<Object> leftExpr, Expression<Object> rightExpr) {
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        Object a = leftExpr != null ? leftExpr.get(ctx) : null;
        Object b = rightExpr != null ? rightExpr.get(ctx) : null;
        // Numeric equality so 0.0 and 0 compare equal (e.g. "assert {_num} is 0" after random number between 0 and 0)
        if (a instanceof Number na && b instanceof Number nb) {
            return na.doubleValue() == nb.doubleValue() && Double.isFinite(na.doubleValue()) && Double.isFinite(nb.doubleValue());
        }
        return Objects.equals(a, b);
    }
}
