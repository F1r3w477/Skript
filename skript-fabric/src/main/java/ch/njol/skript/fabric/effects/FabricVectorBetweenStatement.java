package ch.njol.skript.fabric.effects;

import ch.njol.skript.core.event.EventValues;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.core.variables.VariableRef;
import ch.njol.skript.fabric.platform.FabricVector;
import ch.njol.skript.platform.SkriptLocation;

/**
 * Fabric effect: set %variable% to vector between %location% and %location%.
 */
public final class FabricVectorBetweenStatement implements Statement {

    private final Object variableExpr;
    private final Object fromExpr;
    private final Object toExpr;

    public FabricVectorBetweenStatement(Object variableExpr, Object fromExpr, Object toExpr) {
        this.variableExpr = variableExpr;
        this.fromExpr = fromExpr;
        this.toExpr = toExpr;
    }

    @Override
    public void run(ExecutionContext ctx) {
        if (!(variableExpr instanceof VariableRef ref)) return;
        SkriptLocation from = EventValues.resolveLocation(fromExpr, ctx);
        SkriptLocation to = EventValues.resolveLocation(toExpr, ctx);
        if (from == null || to == null) return;
        double dx = to.getBlockX() - from.getBlockX();
        double dy = to.getBlockY() - from.getBlockY();
        double dz = to.getBlockZ() - from.getBlockZ();
        FabricVector vec = new FabricVector(dx, dy, dz);
        ctx.getVariableScope().set(ref.getName(), vec);
    }
}
