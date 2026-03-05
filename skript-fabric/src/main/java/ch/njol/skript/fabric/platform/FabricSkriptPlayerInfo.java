package ch.njol.skript.fabric.platform;

import ch.njol.skript.platform.SkriptPlayerInfo;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric adapter: wraps a {@link ServerPlayer} as {@link SkriptPlayerInfo}.
 */
public final class FabricSkriptPlayerInfo implements SkriptPlayerInfo {

    private final String id;
    private final String name;

    public FabricSkriptPlayerInfo(ServerPlayer player) {
        this.id = player.getUUID().toString();
        this.name = player.getName().getString();
    }

    public FabricSkriptPlayerInfo(String id, String name) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }
}
