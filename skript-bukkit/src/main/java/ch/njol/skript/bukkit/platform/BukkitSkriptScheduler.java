package ch.njol.skript.bukkit.platform;

import ch.njol.skript.platform.SkriptScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Bukkit-backed implementation of the shared {@link SkriptScheduler}
 * abstraction used by the core engine.
 */
public final class BukkitSkriptScheduler implements SkriptScheduler {

    private final Plugin plugin;

    public BukkitSkriptScheduler(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void executeSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}

