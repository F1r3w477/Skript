package ch.njol.skript.fabric.effects;

import ch.njol.skript.core.event.EventValues;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.fabric.platform.FabricSkriptPlatform;
import ch.njol.skript.fabric.platform.FabricSkriptWorld;
import ch.njol.skript.platform.SkriptLocation;
import ch.njol.skript.platform.SkriptWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;

/**
 * Fabric implementation of spawn effect: spawn %entity% at %location%.
 */
public final class FabricSpawnStatement implements Statement {

    private final Object entityExpr;
    private final Object locationExpr;

    public FabricSpawnStatement(Object entityExpr, Object locationExpr) {
        this.entityExpr = entityExpr;
        this.locationExpr = locationExpr;
    }

    @Override
    public void run(ExecutionContext ctx) {
        Object entityVal = EventValues.resolve(entityExpr, ctx);
        Object locVal = EventValues.resolve(locationExpr, ctx);
        if (entityVal == null) return;
        EntityType<?> entityType = resolveEntityType(String.valueOf(entityVal));
        if (entityType == null) return;
        BlockPos pos = resolveBlockPos(locVal);
        ServerLevel level = resolveServerLevel(locVal);
        if (pos == null || level == null) return;
        net.minecraft.world.entity.Entity entity = entityType.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (entity != null) {
            entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(entity);
        }
    }

    private static EntityType<?> resolveEntityType(String s) {
        if (s == null || s.isBlank()) return null;
        String id = s.trim().toLowerCase().replace(' ', '_');
        if (!id.contains(":")) id = "minecraft:" + id;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(type).toString().equals(id)) {
                return type;
            }
        }
        return null;
    }

    private static BlockPos resolveBlockPos(Object locVal) {
        if (locVal instanceof SkriptLocation sl) {
            return new BlockPos(sl.getBlockX(), sl.getBlockY(), sl.getBlockZ());
        }
        return null;
    }

    private static ServerLevel resolveServerLevel(Object locVal) {
        if (locVal instanceof SkriptLocation sl) {
            SkriptWorld w = sl.getWorld();
            ServerLevel level = null;
            if (w instanceof FabricSkriptWorld fw) {
                level = fw.getServerLevel();
            }
            if (level == null) {
                var platform = ch.njol.skript.core.SkriptBootstrap.getPlatform();
                if (platform instanceof FabricSkriptPlatform fabric) {
                    MinecraftServer server = fabric.getServer();
                    if (server != null) {
                        level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                    }
                }
            }
            return level;
        }
        var platform = ch.njol.skript.core.SkriptBootstrap.getPlatform();
        if (platform instanceof FabricSkriptPlatform fabric) {
            MinecraftServer server = fabric.getServer();
            return server != null ? server.getLevel(net.minecraft.world.level.Level.OVERWORLD) : null;
        }
        return null;
    }
}
