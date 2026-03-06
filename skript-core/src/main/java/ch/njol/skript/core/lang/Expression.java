package ch.njol.skript.core.lang;

/**
 * Produces a value at runtime. Used by conditions and effects so that
 * placeholders (literals, event values, variables) are evaluated when
 * {@link #get(ExecutionContext)} is called. No Bukkit types.
 *
 * @param <T> the type of value produced (use {@link Object} for untyped)
 */
public interface Expression<T> {

    /**
     * Evaluate this expression in the current execution context.
     *
     * @param ctx execution context (may be null in tests)
     * @return the value, or null if not set / not applicable
     */
    T get(ExecutionContext ctx);
}
