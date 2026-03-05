package ch.njol.skript.platform;

/**
 * Minimal scheduling abstraction for running work on the main server thread.
 *
 * Implementations may choose to only support {@link #executeSync(Runnable)};
 * delayed and repeating scheduling have default implementations that can be
 * overridden once needed.
 */
public interface SkriptScheduler {

    /**
     * Execute the given task on the platform's main/server thread as soon as possible.
     */
    void executeSync(Runnable task);

    /**
     * Schedule the given task to run once after the given delay (in ticks).
     */
    default void scheduleSyncDelayed(Runnable task, long delayTicks) {
        throw new UnsupportedOperationException("Delayed scheduling is not implemented for this platform");
    }

    /**
     * Schedule the given task to run repeatedly every given number of ticks.
     */
    default void scheduleSyncRepeating(Runnable task, long periodTicks) {
        throw new UnsupportedOperationException("Repeating scheduling is not implemented for this platform");
    }
}

