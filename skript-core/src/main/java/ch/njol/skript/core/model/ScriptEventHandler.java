package ch.njol.skript.core.model;

import ch.njol.skript.core.lang.Statement;

import java.util.Collections;
import java.util.List;

/**
 * Representation of a single "on <event>:" section in a script.
 * Body is a list of parsed {@link Statement}s (effects) executed in order.
 */
public final class ScriptEventHandler {

    private final String eventName;
    private final List<Statement> statements;

    public ScriptEventHandler(String eventName, List<Statement> statements) {
        this.eventName = eventName;
        this.statements = List.copyOf(statements);
    }

    public String getEventName() {
        return eventName;
    }

    public List<Statement> getStatements() {
        return Collections.unmodifiableList(statements);
    }
}

