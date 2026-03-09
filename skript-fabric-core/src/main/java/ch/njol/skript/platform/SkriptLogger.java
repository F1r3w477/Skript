package ch.njol.skript.platform;

/**
 * Minimal logging abstraction used by the shared Skript core.
 * Implementations should be backed by the host platform's logger.
 */
public interface SkriptLogger {

    void info(String message);

    void warn(String message);

    void error(String message);

    void error(String message, Throwable throwable);
}

