package ch.njol.skript.core.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Simple in-memory representation of a single .sk script file.
 */
public final class ScriptFile {

    private final Path path;
    private final List<ScriptEventHandler> eventHandlers;
    private final List<String> testNames;

    public ScriptFile(Path path, List<ScriptEventHandler> eventHandlers, List<String> testNames) {
        this.path = path;
        this.eventHandlers = List.copyOf(eventHandlers);
        this.testNames = List.copyOf(testNames);
    }

    public Path getPath() {
        return path;
    }

    public List<ScriptEventHandler> getEventHandlers() {
        return Collections.unmodifiableList(eventHandlers);
    }

    public List<String> getTestNames() {
        return Collections.unmodifiableList(testNames);
    }
}

