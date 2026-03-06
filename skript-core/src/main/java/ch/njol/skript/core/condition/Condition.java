package ch.njol.skript.core.condition;

import ch.njol.skript.core.lang.ExecutionContext;

/**
 * Platform-agnostic condition: evaluates to true or false in an execution context.
 * No Bukkit types. Used by conditional trigger items (e.g. if/else).
 */
@FunctionalInterface
public interface Condition {

    /**
     * Evaluate this condition.
     *
     * @param ctx execution context
     * @return true if the condition holds
     */
    boolean check(ExecutionContext ctx);
}
