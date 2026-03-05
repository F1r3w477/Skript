package ch.njol.skript.fabric.platform;

import ch.njol.skript.platform.SkriptWorld;

/**
 * Fabric adapter: holds dimension key and name (e.g. from RegistryKey.getValue().toString()).
 */
public final class FabricSkriptWorld implements SkriptWorld {

    private final String key;
    private final String name;

    public FabricSkriptWorld(String key, String name) {
        this.key = key != null ? key : "minecraft:overworld";
        this.name = name != null ? name : "world";
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getName() {
        return name;
    }
}
