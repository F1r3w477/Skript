package ch.njol.skript.bukkit.platform;

import ch.njol.skript.platform.SkriptLocation;
import ch.njol.skript.platform.SkriptWorld;
import org.bukkit.Location;

/**
 * Bukkit adapter: wraps a {@link Location} as {@link SkriptLocation}.
 */
public final class BukkitSkriptLocation implements SkriptLocation {

    private final Location location;

    public BukkitSkriptLocation(Location location) {
        this.location = location;
    }

    @Override
    public SkriptWorld getWorld() {
        return location.getWorld() != null ? new BukkitSkriptWorld(location.getWorld()) : null;
    }

    @Override
    public int getBlockX() {
        return location.getBlockX();
    }

    @Override
    public int getBlockY() {
        return location.getBlockY();
    }

    @Override
    public int getBlockZ() {
        return location.getBlockZ();
    }
}
