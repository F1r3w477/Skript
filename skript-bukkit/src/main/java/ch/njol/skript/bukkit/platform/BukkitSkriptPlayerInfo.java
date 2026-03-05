package ch.njol.skript.bukkit.platform;

import ch.njol.skript.platform.SkriptPlayerInfo;
import org.bukkit.entity.Player;

/**
 * Bukkit adapter: wraps a {@link Player} as {@link SkriptPlayerInfo}.
 */
public final class BukkitSkriptPlayerInfo implements SkriptPlayerInfo {

    private final Player player;

    public BukkitSkriptPlayerInfo(Player player) {
        this.player = player;
    }

    @Override
    public String getId() {
        return player.getUniqueId().toString();
    }

    @Override
    public String getName() {
        return player.getName();
    }
}
