package ch.njol.skript.core.lang.trigger;

import ch.njol.skript.core.lang.ExecutionContext;

/**
 * One item in a trigger chain. Executes and returns the next item to run.
 * Platform-agnostic; no Bukkit Event. Used by {@link SkriptRuntime}.
 */
public abstract class CoreTriggerItem {

    /**
     * Execute this item and return the next item to run.
     *
     * @param ctx execution context
     * @return next item to run, or null to stop
     */
    public abstract CoreTriggerItem run(ExecutionContext ctx);

    /**
     * Walk the chain starting at {@code start}, running each item until null or exception.
     *
     * @param start first item
     * @param ctx   execution context
     * @return false if an exception occurred
     */
    public static boolean walk(CoreTriggerItem start, ExecutionContext ctx) {
        CoreTriggerItem current = start;
        try {
            while (current != null) {
                current = current.run(ctx);
            }
            return true;
        } catch (Exception e) {
            ctx.getLogger().error("Trigger execution failed", e);
            return false;
        }
    }
}
