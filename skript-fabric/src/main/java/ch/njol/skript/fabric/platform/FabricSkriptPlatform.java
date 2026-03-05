package ch.njol.skript.fabric.platform;

import ch.njol.skript.platform.SkriptCommandExecutor;
import ch.njol.skript.platform.SkriptLogger;
import ch.njol.skript.platform.SkriptPlatform;
import ch.njol.skript.platform.SkriptScheduler;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Fabric implementation of the core Skript platform abstraction.
 */
public final class FabricSkriptPlatform implements SkriptPlatform {

    private final SkriptLogger logger;
    private final SkriptScheduler scheduler;
    private final Path configDirectory;
    private final Path scriptsDirectory;
    private volatile SkriptCommandExecutor skriptCommandExecutor;

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
}

