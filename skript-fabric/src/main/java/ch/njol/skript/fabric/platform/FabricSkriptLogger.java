package ch.njol.skript.fabric.platform;

import ch.njol.skript.platform.SkriptLogger;
import org.apache.logging.log4j.Logger;

/**
 * Fabric-backed implementation of the core logging abstraction.
 */
public final class FabricSkriptLogger implements SkriptLogger {

    private final Logger logger;

    public FabricSkriptLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        if (throwable == null) {
            logger.error(message);
        } else {
            logger.error(message, throwable);
        }
    }
}

