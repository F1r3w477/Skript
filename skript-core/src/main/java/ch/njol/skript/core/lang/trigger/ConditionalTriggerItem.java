package ch.njol.skript.core.lang.trigger;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;

/**
 * Trigger item that evaluates a condition and runs either the "then" or "else" chain.
 */
public final class ConditionalTriggerItem extends CoreTriggerItem {

    private final Condition condition;
    private final CoreTriggerItem thenItem;
    private final CoreTriggerItem elseItem;

    /** When true, CondCompare treats both-null as false (so else branch runs on failed resolution). ThreadLocal so it works in any context. */
    public static final ThreadLocal<Boolean> STRICT_NULL_COMPARISON = ThreadLocal.withInitial(() -> false);

    public ConditionalTriggerItem(Condition condition, CoreTriggerItem thenItem, CoreTriggerItem elseItem) {
        this.condition = condition;
        this.thenItem = thenItem;
        this.elseItem = elseItem;
    }

    @Override
    public CoreTriggerItem run(ExecutionContext ctx) {
        boolean trace = Boolean.getBoolean("skript.fabric.trace");
        if (trace) {
            ctx.getLogger().info("[FabricTrace] ConditionalTriggerItem.run: entering, setting STRICT_NULL_COMPARISON=true");
        }
        STRICT_NULL_COMPARISON.set(true);
        try {
            boolean result = condition.check(ctx);
            if (trace) {
                ctx.getLogger().info("[FabricTrace] ConditionalTriggerItem.run: condition.check=" + result + ", returning " + (result ? "then" : "else") + " branch");
            }
            return result ? thenItem : elseItem;
        } finally {
            STRICT_NULL_COMPARISON.remove();
            if (trace) {
                ctx.getLogger().info("[FabricTrace] ConditionalTriggerItem.run: STRICT_NULL_COMPARISON removed");
            }
        }
    }
}
