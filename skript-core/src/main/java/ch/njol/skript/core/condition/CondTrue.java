package ch.njol.skript.core.condition;

import ch.njol.skript.core.lang.ExecutionContext;

/**
 * Condition that always returns true. Used for "if true:" in scripts.
 */
public enum CondTrue implements Condition {
    INSTANCE;

    @Override
    public boolean check(ExecutionContext ctx) {
        return true;
    }
}
