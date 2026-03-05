package ch.njol.skript.bukkit.platform;

import ch.njol.skript.core.SkriptBootstrap;
import ch.njol.skript.platform.SkriptCommandExecutor;
import ch.njol.skript.platform.SkriptLogger;
import ch.njol.skript.platform.SkriptPlatform;
import ch.njol.skript.platform.SkriptPlayerInfo;
import ch.njol.skript.platform.SkriptScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin {@link SkriptPlatform} implementation that adapts the legacy Bukkit
 * plugin environment to the shared core engine. This class intentionally has
 * no direct dependency on the main {@code ch.njol.skript.Skript} plugin
 * class so that it can be evolved independently.
 */
public final class BukkitSkriptPlatform implements SkriptPlatform {

    private final Plugin plugin;
    private final SkriptLogger logger;
    private final SkriptScheduler scheduler;

    public BukkitSkriptPlatform(Plugin plugin, SkriptLogger logger, SkriptScheduler scheduler) {
        this.plugin = plugin;
        this.logger = logger;
        this.scheduler = scheduler;
    }

    @Override
    public String getName() {
        return "Bukkit";
    }

    @Override
    public SkriptLogger getLogger() {
        return logger;
    }

    @Override
    public SkriptScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public Path getConfigDirectory() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public Path getScriptsDirectory() {
        return getConfigDirectory().resolve("scripts");
    }

    @Override
    public void registerCommand(String name, String description, SkriptCommandExecutor executor) {
        CommandMap map = plugin.getServer().getCommandMap();
        Command command = new Command(name, description, "/" + name + " [args]", List.of()) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                executor.execute(new BukkitSkriptCommandSender(sender), args);
                return true;
            }
        };
        map.register(plugin.getName().toLowerCase(), command);
    }

    @Override
    public Collection<SkriptPlayerInfo> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream()
            .map(BukkitSkriptPlayerInfo::new)
            .collect(Collectors.toList());
    }

    /**
     * Convenience bootstrap for wiring the shared core into the running
     * Bukkit server. This can be invoked from the main plugin once the
     * server and configuration are ready.
     */
    public static void startCore(Plugin plugin, SkriptLogger logger, SkriptScheduler scheduler) {
        BukkitSkriptPlatform platform = new BukkitSkriptPlatform(plugin, logger, scheduler);
        SkriptBootstrap.start(platform);
    }
}

