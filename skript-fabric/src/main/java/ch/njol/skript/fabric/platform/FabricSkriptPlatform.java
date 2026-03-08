package ch.njol.skript.fabric.platform;

import ch.njol.skript.core.syntax.SyntaxRegistry;
import ch.njol.skript.core.types.CoreClassInfo;
import ch.njol.skript.core.types.CoreTypes;
import ch.njol.skript.core.types.ParseContext;
import ch.njol.skript.platform.SkriptCommandExecutor;
import ch.njol.skript.platform.SkriptLogger;
import ch.njol.skript.core.lang.Expressions;
import ch.njol.skript.fabric.conditions.CondFabricVectorEquals;
import ch.njol.skript.fabric.effects.FabricClearEntityStatement;
import ch.njol.skript.fabric.effects.FabricExecuteConsoleCommandStatement;
import ch.njol.skript.fabric.effects.FabricKillStatement;
import ch.njol.skript.fabric.effects.FabricLoadScriptStatement;
import ch.njol.skript.fabric.effects.FabricSetBlockStatement;
import ch.njol.skript.fabric.effects.FabricSpawnStatement;
import ch.njol.skript.fabric.effects.FabricVectorBetweenStatement;
import ch.njol.skript.fabric.effects.FabricVectorFromXYZStatement;
import ch.njol.skript.fabric.core.FabricScriptsDirectory;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Fabric implementation of the core Skript platform abstraction.
 */
public final class FabricSkriptPlatform implements SkriptPlatform {

    private final SkriptLogger logger;
    private final SkriptScheduler scheduler;
    private final Path configDirectory;
    /** Real script location (config or skript.testing.dir). */
    private final Path realScriptsPath;
    /** Stable directory for preprocessed scripts; core loads from here. */
    private final Path scriptsWorkDir;
    private volatile SkriptCommandExecutor skriptCommandExecutor;
    private volatile MinecraftServer server;
    /** Script-defined commands (e.g. "testing1") for "on command /name:". */
    private final Map<String, SkriptCommandExecutor> scriptCommands = new ConcurrentHashMap<>();

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
		this.realScriptsPath = scriptsDir;
		this.scriptsWorkDir = baseConfigDir.resolve("scripts-preprocessed");

        try {
            Files.createDirectories(this.configDirectory);
            Files.createDirectories(this.realScriptsPath);
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
        // Re-run preprocessor so reload sees updated content; core loads from work dir.
        return FabricScriptsDirectory.prepareScriptsDirectory(realScriptsPath, scriptsWorkDir, logger);
    }

    @Override
    public boolean isPluginEnabled(String name) {
        if (name == null || name.isBlank()) return false;
        return "Skript".equalsIgnoreCase(name) || "skript-fabric".equalsIgnoreCase(name);
    }

    @Override
    public void registerCommand(String name, String description, SkriptCommandExecutor executor) {
        if ("skript".equalsIgnoreCase(name)) {
            this.skriptCommandExecutor = executor;
        } else if (name != null && !name.isBlank()) {
            scriptCommands.put(name.toLowerCase(), executor);
        }
    }

    /**
     * Script-defined commands registered via "on command /name:". Used by Fabric command registration
     * to register each with Brigadier so they are executable.
     */
    public Map<String, SkriptCommandExecutor> getScriptCommands() {
        return Collections.unmodifiableMap(scriptCommands);
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

    /**
     * The current Minecraft server; null before server start.
     */
    public MinecraftServer getServer() {
        return server;
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
    public void registerPlatformEffects(SyntaxRegistry registry) {
        registry.registerEffectFirst("execute console command %string%", m -> new FabricExecuteConsoleCommandStatement(m.getExpression(0)));
        registry.registerStatementFirst("execute console command %string%", m -> new FabricExecuteConsoleCommandStatement(m.getExpression(0)));
        registry.registerEffectFirst("spawn %object% at %object%", m -> new FabricSpawnStatement(m.getExpression(0), m.getExpression(1)));
        registry.registerEffectFirst("spawn < at > at %object%", m -> new FabricSpawnStatement(m.getExpression(0), m.getExpression(1)));
        registry.registerEffectFirst("set block at %object% to %object%", m -> new FabricSetBlockStatement(m.getExpression(0), m.getExpression(1)));
        registry.registerEffectFirst("kill %object%", m -> new FabricKillStatement(m.getExpression(0)));
        registry.registerEffectFirst("clear entity within %object%", m -> new FabricClearEntityStatement(m.getExpression(0)));
        registry.registerEffectFirst("clear all entities", m -> new FabricClearEntityStatement(null));
        registry.registerStatementFirst("spawn %object% at %object%", m -> new FabricSpawnStatement(m.getExpression(0), m.getExpression(1)));
        registry.registerStatementFirst("spawn < at > at %object%", m -> new FabricSpawnStatement(m.getExpression(0), m.getExpression(1)));
        registry.registerStatementFirst("set block at %object% to %object%", m -> new FabricSetBlockStatement(m.getExpression(0), m.getExpression(1)));
        registry.registerStatementFirst("kill %object%", m -> new FabricKillStatement(m.getExpression(0)));
        registry.registerStatementFirst("clear entity within %object%", m -> new FabricClearEntityStatement(m.getExpression(0)));
        registry.registerStatementFirst("clear all entities", m -> new FabricClearEntityStatement(null));
        // Load/reload script: full reload + fire "script load" so "on script load:" handlers run (e.g. EffScriptFile tests)
        registry.registerEffectFirst("load script named %variable%", m -> new FabricLoadScriptStatement(m.getExpression(0)));
        registry.registerEffectFirst("reload script named %variable%", m -> new FabricLoadScriptStatement(m.getExpression(0)));
        registry.registerEffectFirst("load script [file] %variable%", m -> new FabricLoadScriptStatement(m.getExpression(0)));
        registry.registerEffectFirst("reload script [file] %variable%", m -> new FabricLoadScriptStatement(m.getExpression(0)));
        registry.registerEffectFirst("reload (script named %variable%)", m -> new FabricLoadScriptStatement(m.getExpression(0)));
        registry.registerStatementFirst("load script named %variable%", m -> new FabricLoadScriptStatement(m.getExpression(0)));
        registry.registerStatementFirst("reload script named %variable%", m -> new FabricLoadScriptStatement(m.getExpression(0)));
        registry.registerStatementFirst("load script [file] %variable%", m -> new FabricLoadScriptStatement(m.getExpression(0)));
        registry.registerStatementFirst("reload script [file] %variable%", m -> new FabricLoadScriptStatement(m.getExpression(0)));
        registry.registerStatementFirst("reload (script named %variable%)", m -> new FabricLoadScriptStatement(m.getExpression(0)));
        // Vector effects (Phase 7): set var to vector between/from xyz
        registry.registerEffectFirst("set %variable% to vector between %object% and %object%", m ->
            new FabricVectorBetweenStatement(m.getExpression(0), m.getExpression(1), m.getExpression(2)));
        registry.registerStatementFirst("set %variable% to vector between %object% and %object%", m ->
            new FabricVectorBetweenStatement(m.getExpression(0), m.getExpression(1), m.getExpression(2)));
        registry.registerEffectFirst("set %variable% to a new vector from %object%, %object% and %object%", m ->
            new FabricVectorFromXYZStatement(m.getExpression(0), m.getExpression(1), m.getExpression(2), m.getExpression(3)));
        registry.registerStatementFirst("set %variable% to a new vector from %object%, %object% and %object%", m ->
            new FabricVectorFromXYZStatement(m.getExpression(0), m.getExpression(1), m.getExpression(2), m.getExpression(3)));
        registry.registerEffectFirst("set %variable% to a new vector from %object%, %object%, %object%", m ->
            new FabricVectorFromXYZStatement(m.getExpression(0), m.getExpression(1), m.getExpression(2), m.getExpression(3)));
        registry.registerStatementFirst("set %variable% to a new vector from %object%, %object%, %object%", m ->
            new FabricVectorFromXYZStatement(m.getExpression(0), m.getExpression(1), m.getExpression(2), m.getExpression(3)));
        registry.registerEffectFirst("set %variable% to vector from %object%, %object%, %object%", m ->
            new FabricVectorFromXYZStatement(m.getExpression(0), m.getExpression(1), m.getExpression(2), m.getExpression(3)));
        registry.registerStatementFirst("set %variable% to vector from %object%, %object%, %object%", m ->
            new FabricVectorFromXYZStatement(m.getExpression(0), m.getExpression(1), m.getExpression(2), m.getExpression(3)));
        // Condition: X is vector(a,b,c) - core has no registerConditionFirst so generic "%object% is %object%" may match first
        registry.registerCondition("%-object% is vector(%-number%, %-number%, %-number%)", m ->
            new CondFabricVectorEquals(
                Expressions.fromParsed(m.getExpression(0)),
                Expressions.fromParsed(m.getExpression(1)),
                Expressions.fromParsed(m.getExpression(2)),
                Expressions.fromParsed(m.getExpression(3))));
    }

    @Override
    public void registerTypes(CoreTypes types) {
        types.register(new CoreClassInfo<>("player", Object.class, (s, ctx) -> {
            if (s != null && "event-player".equalsIgnoreCase(s.trim())) {
                return ch.njol.skript.core.event.EventValue.PLAYER;
            }
            return resolvePlayer(s);
        }));
    }
}

