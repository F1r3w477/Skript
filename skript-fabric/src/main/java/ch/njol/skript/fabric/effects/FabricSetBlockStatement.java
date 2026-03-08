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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fabric implementation of set block effect: set block at %location% to %block%.
 */
public final class FabricSetBlockStatement implements Statement {

    private final Object locationExpr;
    private final Object blockExpr;

    public FabricSetBlockStatement(Object locationExpr, Object blockExpr) {
        this.locationExpr = locationExpr;
        this.blockExpr = blockExpr;
    }

    @Override
    public void run(ExecutionContext ctx) {
        Object locVal = EventValues.resolve(locationExpr, ctx);
        Object blockVal = EventValues.resolve(blockExpr, ctx);
        if (locVal == null || blockVal == null) return;
        BlockPos pos = resolveBlockPos(locVal);
        ServerLevel level = resolveServerLevel(locVal);
        Block block = resolveBlock(String.valueOf(blockVal));
        if (pos == null || level == null || block == null) return;
        BlockState blockState = block.defaultBlockState();
        level.setBlock(pos, blockState, 3);
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

    private static Block resolveBlock(String s) {
        if (s == null || s.isBlank()) return null;
        String id = s.trim().toLowerCase().replace(' ', '_');
        if (!id.contains(":")) id = "minecraft:" + id;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (BuiltInRegistries.BLOCK.getKey(block).toString().equals(id)) {
                return block;
            }
        }
        return null;
    }
}
