package ch.njol.skript.core.condition;

import ch.njol.skript.core.lang.ExecutionContext;

/**
 * Condition that always returns false. Used for "if false:" in scripts.
 */
public enum CondFalse implements Condition {
    INSTANCE;

    @Override
    public boolean check(ExecutionContext ctx) {
        return false;
    }
}
