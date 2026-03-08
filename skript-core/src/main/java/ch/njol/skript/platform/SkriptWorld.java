package ch.njol.skript.platform;

/**
 * Platform-agnostic view of a world/dimension.
 * Implementations wrap Bukkit World or Fabric ServerWorld without exposing platform types.
 */
public interface SkriptWorld {

    /**
     * Stable key for this world (e.g. "minecraft:overworld"). Never null.
     */
    String getKey();

    /**
     * Human-readable name (e.g. "world"). Never null.
     */
    String getName();

    /**
     * Platform-specific world object (e.g. ServerLevel, World). May be null if not available.
     */
    default Object getPlatformWorld() {
        return null;
    }
}
