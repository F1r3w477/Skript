package ch.njol.skript.platform;

import ch.njol.skript.core.syntax.SyntaxRegistry;
import ch.njol.skript.core.types.CoreTypes;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

/**
 * Host-platform abstraction used by the Skript core.
 * <p>
 * Implementations are responsible for bridging into Bukkit, Fabric, etc.
 * The core uses this interface to obtain directories, logger, scheduler,
 * and to register commands or resolve players without depending on platform types.
 */
public interface SkriptPlatform {

    /**
     * Human-readable name of the platform, e.g. "Bukkit" or "Fabric".
     */
    String getName();

    /**
     * Logger for core messages (script output, errors, reload notices).
     */
    SkriptLogger getLogger();

    /**
     * Scheduler for running work on the main/server thread. Used for
     * delayed and repeating script tasks.
     */
    SkriptScheduler getScheduler();

    /**
     * Root configuration directory for Skript on this platform.
     * Typically something like <code>plugins/Skript</code> (Bukkit)
     * or <code>config/skript</code> (Fabric).
     */
    Path getConfigDirectory();

    /**
     * Directory that contains user scripts (.sk files). The core discovers
     * and loads scripts from this path via {@link ch.njol.skript.core.SkriptBootstrap}.
     */
    Path getScriptsDirectory();

    /**
     * Register a command that the core can use (e.g. /skript reload).
     * Default implementation does nothing; platforms override when they support commands.
     *
     * @param name        command name (lowercase, no slash)
     * @param description short description for help/usage
     * @param executor    callback when the command is run
     */
    default void registerCommand(String name, String description, SkriptCommandExecutor executor) {
    }

    /**
     * Return a snapshot of currently online players for use in event context
     * or script logic. Default returns empty list; platforms override when available.
     */
    default Collection<SkriptPlayerInfo> getOnlinePlayers() {
        return Collections.emptyList();
    }

    /**
     * Whether the given player is a server operator. Default false; platforms override.
     */
    default boolean isOp(SkriptPlayerInfo player) {
        return false;
    }

    /**
     * Resolve a player by name (or identifier). Used when parsing %player% in patterns.
     * Default returns null; platforms override to resolve from online/offline players.
     */
    default SkriptPlayerInfo resolvePlayer(String name) {
        return null;
    }

    /**
     * Send a message to a player. If player is null, default implementation logs the message.
     */
    default void sendMessage(SkriptPlayerInfo player, String message) {
        // Default: no player target, could log; implementations override to send to player
    }

    /**
     * Whether a plugin (or mod) with the given name is enabled. Used for "parse if plugin X is enabled".
     * Default false; platforms override (e.g. Fabric returns true for "Skript").
     */
    default boolean isPluginEnabled(String name) {
        return false;
    }

    /**
     * Register platform-specific types (e.g. "player") with the core type system.
     * Called by the engine before loading scripts so that patterns like %player% can be parsed.
     */
    default void registerTypes(CoreTypes types) {
    }

    /**
     * Register platform-specific effects (e.g. spawn, kill, set block). Called before loading scripts.
     * Platforms add their implementations; use {@link SyntaxRegistry#registerEffectFirst} so they
     * take precedence over core NoOp fallbacks.
     */
    default void registerPlatformEffects(SyntaxRegistry registry) {
    }
}

