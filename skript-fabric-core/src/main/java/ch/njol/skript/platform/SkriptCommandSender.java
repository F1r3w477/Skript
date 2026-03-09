package ch.njol.skript.platform;

/**
 * Minimal abstraction of a command sender (player or console).
 * Platforms provide implementations that wrap Bukkit/Fabric types
 * so the core never depends on platform-specific APIs.
 */
public interface SkriptCommandSender {

    /**
     * Display name for logs and messages (e.g. player name or "Console").
     */
    String getName();

    /**
     * Send a message to this sender. No-op for console if the platform
     * does not support console feedback.
     */
    void sendMessage(String message);

    /**
     * True if this sender is the server console, false if a player.
     */
    boolean isConsole();
}
