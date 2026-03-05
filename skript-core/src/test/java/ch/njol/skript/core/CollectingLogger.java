package ch.njol.skript.core;

import ch.njol.skript.platform.SkriptLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test double: captures log messages for assertions.
 */
final class CollectingLogger implements SkriptLogger {

    private final List<String> info = new CopyOnWriteArrayList<>();
    private final List<String> warn = new CopyOnWriteArrayList<>();
    private final List<String> error = new CopyOnWriteArrayList<>();

    @Override
    public void info(String message) {
        info.add(message);
    }

    @Override
    public void warn(String message) {
        warn.add(message);
    }

    @Override
    public void error(String message) {
        error.add(message);
    }

    @Override
    public void error(String message, Throwable t) {
        error.add(message + " " + (t != null ? t.getMessage() : ""));
    }

    List<String> getInfo() {
        return new ArrayList<>(info);
    }

    List<String> getWarn() {
        return new ArrayList<>(warn);
    }

    List<String> getError() {
        return new ArrayList<>(error);
    }

    void clear() {
        info.clear();
        warn.clear();
        error.clear();
    }
}
