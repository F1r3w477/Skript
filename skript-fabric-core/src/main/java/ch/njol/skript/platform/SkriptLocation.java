package ch.njol.skript.platform;

/**
 * Platform-agnostic block location (world + integer coordinates).
 * Implementations wrap Bukkit Location or Fabric BlockPos + world without exposing platform types.
 */
public interface SkriptLocation {

    SkriptWorld getWorld();

    int getBlockX();

    int getBlockY();

    int getBlockZ();
}
