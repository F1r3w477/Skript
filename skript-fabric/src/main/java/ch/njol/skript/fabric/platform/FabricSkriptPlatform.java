package ch.njol.skript.fabric.platform;

import ch.njol.skript.core.types.CoreClassInfo;
import ch.njol.skript.core.types.CoreTypes;
import ch.njol.skript.core.types.ParseContext;
import ch.njol.skript.platform.SkriptCommandExecutor;
import ch.njol.skript.platform.SkriptLogger;
import ch.njol.skript.platform.SkriptPlatform;
import ch.njol.skript.platform.SkriptPlayerInfo;
import ch.njol.skript.platform.SkriptScheduler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fabric implementation of the core Skript platform abstraction.
 */
public final class FabricSkriptPlatform implements SkriptPlatform {

    private final SkriptLogger logger;
    private final SkriptScheduler scheduler;
    private final Path configDirectory;
    private final Path scriptsDirectory;
    private volatile SkriptCommandExecutor skriptCommandExecutor;
    private volatile MinecraftServer server;

    public FabricSkriptPlatform(SkriptLogger logger) {
        this.logger = logger;
        this.scheduler = new FabricSkriptScheduler();

		Path baseConfigDir = FabricLoader.getInstance().getConfigDir().resolve("skript");
		this.configDirectory = baseConfigDir;

		// In normal operation we load scripts from the Fabric config directory.
		// When running under the upstream Skript test runner, honor the
		// system property 'skript.testing.dir' so we load the .sk test suite
		// directly from the host repository instead of config/skript/scripts.
		Path scriptsDir = baseConfigDir.resolve("scripts");
		boolean testingEnabled = Boolean.getBoolean("skript.testing.enabled");
		String testingDirProp = System.getProperty("skript.testing.dir");
		if (testingEnabled && testingDirProp != null && !testingDirProp.isBlank()) {
			try {
				Path testingDir = Paths.get(testingDirProp).toAbsolutePath().normalize();
				scriptsDir = testingDir;
				this.logger.info("Test mode detected; using scripts directory from 'skript.testing.dir': " + scriptsDir);
			} catch (Exception e) {
				this.logger.error("Failed to resolve skript.testing.dir='" + testingDirProp + "', falling back to default scripts directory.", e);
			}
		}
		this.scriptsDirectory = scriptsDir;

        try {
            Files.createDirectories(this.configDirectory);
            Files.createDirectories(this.scriptsDirectory);
        } catch (Exception e) {
            this.logger.error("Failed to create Skript config or scripts directories at " + this.configDirectory, e);
        }
    }

    @Override
    public String getName() {
        return "Fabric";
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
        return configDirectory;
    }

    @Override
    public Path getScriptsDirectory() {
        return scriptsDirectory;
    }

    @Override
    public void registerCommand(String name, String description, SkriptCommandExecutor executor) {
        if ("skript".equalsIgnoreCase(name)) {
            this.skriptCommandExecutor = executor;
        }
        // Actual Fabric command registration happens in Phase 4; executor is wired then.
    }

    /**
     * Used by the Fabric /skript command (when registered) to delegate to the core executor.
     */
    public SkriptCommandExecutor getSkriptCommandExecutor() {
        return skriptCommandExecutor;
    }

    /**
     * Set by the event bridge when the server starts. Used for getOnlinePlayers, resolvePlayer, isOp.
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public Collection<SkriptPlayerInfo> getOnlinePlayers() {
        MinecraftServer s = server;
        if (s == null) return Collections.emptyList();
        return s.getPlayerList().getPlayers().stream()
            .map(FabricSkriptPlayerInfo::new)
            .collect(Collectors.toList());
    }

    @Override
    public boolean isOp(SkriptPlayerInfo player) {
        // TODO: use ServerOpList with correct 1.21 API (NameAndId) when available
        return false;
    }

    @Override
    public SkriptPlayerInfo resolvePlayer(String name) {
        if (name == null || name.isBlank()) return null;
        MinecraftServer s = server;
        if (s == null) return null;
        var player = s.getPlayerList().getPlayerByName(name);
        return player != null ? new FabricSkriptPlayerInfo(player) : null;
    }

    @Override
    public void sendMessage(SkriptPlayerInfo player, String message) {
        if (message == null) return;
        MinecraftServer s = server;
        if (s == null) {
            logger.info("[send] " + message);
            return;
        }
        if (player == null) {
            logger.info("[send] " + message);
            return;
        }
        try {
            var serverPlayer = s.getPlayerList().getPlayer(UUID.fromString(player.getId()));
            if (serverPlayer != null) {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
            } else {
                logger.info("[send to " + player.getName() + "] " + message);
            }
        } catch (IllegalArgumentException e) {
            logger.info("[send to " + player.getName() + "] " + message);
        }
    }

    @Override
    public void registerTypes(CoreTypes types) {
        types.register(new CoreClassInfo<>("player", SkriptPlayerInfo.class,
            (s, ctx) -> resolvePlayer(s)));
    }
}

