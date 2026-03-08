package ch.njol.skript.fabric.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;
import ch.njol.skript.fabric.platform.FabricVector;

/**
 * Condition: %object% is vector(%object%, %object%, %object%).
 * Compares left value (FabricVector) to vector built from three numbers.
 */
public final class CondFabricVectorEquals implements Condition {

    private final Expression<Object> leftExpr;
    private final Expression<Object> xExpr;
    private final Expression<Object> yExpr;
    private final Expression<Object> zExpr;

    public CondFabricVectorEquals(Expression<Object> leftExpr, Expression<Object> xExpr, Expression<Object> yExpr, Expression<Object> zExpr) {
        this.leftExpr = leftExpr;
        this.xExpr = xExpr;
        this.yExpr = yExpr;
        this.zExpr = zExpr;
    }

    private static double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o == null) return 0;
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        Object left = leftExpr != null ? leftExpr.get(ctx) : null;
        if (!(left instanceof FabricVector vec)) return false;
        double x = toDouble(xExpr != null ? xExpr.get(ctx) : null);
        double y = toDouble(yExpr != null ? yExpr.get(ctx) : null);
        double z = toDouble(zExpr != null ? zExpr.get(ctx) : null);
        return vec.equals(new FabricVector(x, y, z));
    }
}
