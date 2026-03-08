package ch.njol.skript.fabric.effects;

import ch.njol.skript.core.event.EventValues;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.fabric.platform.FabricSkriptPlatform;
import ch.njol.skript.fabric.platform.FabricSkriptWorld;
import ch.njol.skript.platform.SkriptLocation;
import ch.njol.skript.platform.SkriptWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabric implementation of clear entity: clear entity within %location%, clear all entities.
 */
public final class FabricClearEntityStatement implements Statement {

    private final Object regionExpr;  // null for "clear all entities"

    public FabricClearEntityStatement(Object regionExpr) {
        this.regionExpr = regionExpr;
    }

    @Override
    public void run(ExecutionContext ctx) {
        var platform = ch.njol.skript.core.SkriptBootstrap.getPlatform();
        if (!(platform instanceof FabricSkriptPlatform fabric)) return;
        MinecraftServer server = fabric.getServer();
        if (server == null) return;
        if (regionExpr == null) {
            for (ServerLevel level : server.getAllLevels()) {
                List<Entity> toRemove = new ArrayList<>();
                level.getAllEntities().forEach(e -> {
                    if (!(e instanceof Player)) toRemove.add(e);
                });
                for (Entity e : toRemove) {
                    e.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        } else {
            Object val = EventValues.resolve(regionExpr, ctx);
            if (val instanceof SkriptLocation sl) {
                ServerLevel level = null;
                SkriptWorld w = sl.getWorld();
                if (w instanceof FabricSkriptWorld fw) {
                    level = fw.getServerLevel();
                }
                if (level == null) {
                    level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                }
                if (level != null) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(sl.getBlockX(), sl.getBlockY(), sl.getBlockZ());
                    List<Entity> toRemove = new ArrayList<>();
                    level.getEntities(null, new net.minecraft.world.phys.AABB(pos).inflate(8))
                        .forEach(e -> {
                            if (!(e instanceof Player)) toRemove.add(e);
                        });
                    for (Entity e : toRemove) {
                        e.remove(Entity.RemovalReason.DISCARDED);
                    }
                }
            }
        }
    }
}
