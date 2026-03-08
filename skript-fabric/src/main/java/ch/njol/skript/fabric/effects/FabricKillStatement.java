package ch.njol.skript.fabric.effects;

import ch.njol.skript.core.event.EventValues;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Statement;
import net.minecraft.world.entity.Entity;

/**
 * Fabric implementation of kill effect: kill %entity%.
 */
public final class FabricKillStatement implements Statement {

    private final Object entityExpr;

    public FabricKillStatement(Object entityExpr) {
        this.entityExpr = entityExpr;
    }

    @Override
    public void run(ExecutionContext ctx) {
        Object val = EventValues.resolve(entityExpr, ctx);
        if (val instanceof Entity entity) {
            entity.remove(Entity.RemovalReason.KILLED);
        }
    }
}
