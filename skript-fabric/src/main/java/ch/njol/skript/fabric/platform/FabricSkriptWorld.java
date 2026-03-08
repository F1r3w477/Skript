package ch.njol.skript.fabric.platform;

import ch.njol.skript.platform.SkriptWorld;
import net.minecraft.server.level.ServerLevel;

/**
 * Fabric adapter: holds dimension key and name, optionally the ServerLevel.
 */
public final class FabricSkriptWorld implements SkriptWorld {

    private final String key;
    private final String name;
    private final ServerLevel serverLevel;

    public FabricSkriptWorld(String key, String name) {
        this(key, name, null);
    }

    public FabricSkriptWorld(ServerLevel level) {
        this(level != null ? level.dimension().toString().replace("ResourceKey[minecraft:root / ", "").replace("]", "") : "minecraft:overworld",
            level != null ? "world" : "world", level);
    }

    public FabricSkriptWorld(String key, String name, ServerLevel serverLevel) {
        this.key = key != null ? key : "minecraft:overworld";
        this.name = name != null ? name : "world";
        this.serverLevel = serverLevel;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getPlatformWorld() {
        return serverLevel;
    }

    public ServerLevel getServerLevel() {
        return serverLevel;
    }
}
