package ch.njol.skript.fabric.platform;

import ch.njol.skript.platform.SkriptLocation;
import ch.njol.skript.platform.SkriptWorld;

/**
 * Fabric adapter: holds world reference and block coordinates.
 */
public final class FabricSkriptLocation implements SkriptLocation {

    private final SkriptWorld world;
    private final int blockX;
    private final int blockY;
    private final int blockZ;

    public FabricSkriptLocation(SkriptWorld world, int blockX, int blockY, int blockZ) {
        this.world = world;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }

    @Override
    public SkriptWorld getWorld() {
        return world;
    }

    @Override
    public int getBlockX() {
        return blockX;
    }

    @Override
    public int getBlockY() {
        return blockY;
    }

    @Override
    public int getBlockZ() {
        return blockZ;
    }
}
