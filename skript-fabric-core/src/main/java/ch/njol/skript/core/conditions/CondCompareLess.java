package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;

/**
 * Condition: %number% is less than %number%.
 */
public final class CondCompareLess implements Condition {

    private final Expression<Object> leftExpr;
    private final Expression<Object> rightExpr;

    public CondCompareLess(Expression<Object> leftExpr, Expression<Object> rightExpr) {
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        Object a = leftExpr != null ? leftExpr.get(ctx) : null;
        Object b = rightExpr != null ? rightExpr.get(ctx) : null;
        return compare(a, b) < 0;
    }

    private static int compare(Object a, Object b) {
        double na = toDouble(a);
        double nb = toDouble(b);
        return Double.compare(na, nb);
    }

    private static double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
