package ch.njol.skript.bukkit.platform;

import ch.njol.skript.platform.SkriptLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link SkriptLogger} backed by the Bukkit plugin logger. This provides
 * the minimal surface needed by the shared core engine.
 */
public final class BukkitSkriptLogger implements SkriptLogger {

    private final Logger delegate;

    public BukkitSkriptLogger(Logger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void info(String message) {
        delegate.info(message);
    }

    @Override
    public void warn(String message) {
        delegate.warning(message);
    }

    @Override
    public void error(String message) {
        delegate.severe(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        delegate.log(Level.SEVERE, message, throwable);
    }
}

