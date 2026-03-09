package ch.njol.skript.platform;

/**
 * Minimal, platform-agnostic view of a player (identifier and name).
 * Used for event context and command handling without depending on Bukkit/Fabric types.
 */
public interface SkriptPlayerInfo {

    /**
     * Stable identifier for this player (e.g. UUID string). Never null.
     */
    String getId();

    /**
     * Current display name. Never null.
     */
    String getName();
}
