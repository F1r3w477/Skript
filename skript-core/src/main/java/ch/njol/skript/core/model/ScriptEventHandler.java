package ch.njol.skript.core.model;

import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.core.lang.trigger.CoreTriggerItem;
import ch.njol.skript.core.lang.trigger.StatementTriggerItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of a single "on &lt;event&gt;:" section in a script.
 * Body is executed as a {@link CoreTriggerItem} chain (supports conditionals); optionally
 * holds a flat list of statements for backward compatibility and tests.
 */
public final class ScriptEventHandler {

    private final String eventName;
    private final CoreTriggerItem firstTriggerItem;
    private final List<Statement> statements;
    private final String associatedTestName;

    /**
     * Builds a handler from a trigger chain (primary path from parser).
     */
    public ScriptEventHandler(String eventName, CoreTriggerItem firstTriggerItem) {
        this(eventName, firstTriggerItem, null);
    }

    /**
     * Builds a handler from a trigger chain with an optional test name (for "test \"name\":" sections).
     */
    public ScriptEventHandler(String eventName, CoreTriggerItem firstTriggerItem, String associatedTestName) {
        this.eventName = eventName;
        this.firstTriggerItem = firstTriggerItem;
        this.statements = collectStatements(firstTriggerItem);
        this.associatedTestName = associatedTestName;
    }

    /**
     * Builds a handler from a flat list of statements (tests / backward compat);
     * constructs an internal chain of {@link StatementTriggerItem}s.
     */
    public ScriptEventHandler(String eventName, List<Statement> statements) {
        this.eventName = eventName;
        this.statements = List.copyOf(statements);
        this.firstTriggerItem = chainFromStatements(statements);
        this.associatedTestName = null;
    }

    public String getEventName() {
        return eventName;
    }

    /**
     * First item of the trigger chain. Runtime runs {@link CoreTriggerItem#walk(CoreTriggerItem, ch.njol.skript.core.lang.ExecutionContext)} from here.
     */
    public CoreTriggerItem getFirstTriggerItem() {
        return firstTriggerItem;
    }

    /**
     * Flat list of statements (from chain or constructor). For tests and compatibility.
     */
    public List<Statement> getStatements() {
        return Collections.unmodifiableList(statements);
    }

    /**
     * Test name when this handler was built from a "test \"name\":" section; null otherwise.
     */
    public String getAssociatedTestName() {
        return associatedTestName;
    }

    private static CoreTriggerItem chainFromStatements(List<Statement> list) {
        CoreTriggerItem next = null;
        for (int i = list.size() - 1; i >= 0; i--) {
            next = new StatementTriggerItem(list.get(i), next);
        }
        return next;
    }

    private static List<Statement> collectStatements(CoreTriggerItem start) {
        List<Statement> out = new ArrayList<>();
        CoreTriggerItem cur = start;
        while (cur != null) {
            if (cur instanceof StatementTriggerItem st) {
                out.add(st.getStatement());
                cur = st.getNext();
            } else {
                cur = null;
            }
        }
        return out;
    }
}

