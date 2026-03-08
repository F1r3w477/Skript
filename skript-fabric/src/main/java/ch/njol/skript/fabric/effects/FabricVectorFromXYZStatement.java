package ch.njol.skript.fabric.effects;

import ch.njol.skript.core.event.EventValues;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.core.variables.VariableRef;
import ch.njol.skript.fabric.platform.FabricVector;

/**
 * Fabric effect: set %variable% to [a new] vector from %object%, %object% [and] %object%.
 */
public final class FabricVectorFromXYZStatement implements Statement {

    private final Object variableExpr;
    private final Object xExpr;
    private final Object yExpr;
    private final Object zExpr;

    public FabricVectorFromXYZStatement(Object variableExpr, Object xExpr, Object yExpr, Object zExpr) {
        this.variableExpr = variableExpr;
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
    public void run(ExecutionContext ctx) {
        if (!(variableExpr instanceof VariableRef ref)) return;
        double x = toDouble(EventValues.resolve(xExpr, ctx));
        double y = toDouble(EventValues.resolve(yExpr, ctx));
        double z = toDouble(EventValues.resolve(zExpr, ctx));
        FabricVector vec = new FabricVector(x, y, z);
        ctx.getVariableScope().set(ref.getName(), vec);
    }
}
