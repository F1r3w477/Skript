package ch.njol.skript.bukkit.platform;

import ch.njol.skript.platform.SkriptWorld;
import org.bukkit.World;

/**
 * Bukkit adapter: wraps a {@link World} as {@link SkriptWorld}.
 */
public final class BukkitSkriptWorld implements SkriptWorld {

    private final World world;

    public BukkitSkriptWorld(World world) {
        this.world = world;
    }

    @Override
    public String getKey() {
        return world.getKey().toString();
    }

    @Override
    public String getName() {
        return world.getName();
    }
}
