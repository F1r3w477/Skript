package ch.njol.skript.core.lang.trigger;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;

/**
 * Trigger item that loops while a condition holds. Runs the body chain, then re-evaluates
 * the condition; when false, continues to nextAfter. The body chain's last item must
 * have next pointing back to this item so execution returns here after each iteration.
 */
public final class WhileTriggerItem extends CoreTriggerItem {

    private final Condition condition;
    private final CoreTriggerItem nextAfter;
    private CoreTriggerItem bodyFirst;

    public WhileTriggerItem(Condition condition, CoreTriggerItem nextAfter) {
        this.condition = condition;
        this.nextAfter = nextAfter;
    }

    /**
     * Set the first item of the loop body (call after building the body chain with this as next).
     */
    public void setBodyFirst(CoreTriggerItem bodyFirst) {
        this.bodyFirst = bodyFirst;
    }

    @Override
    public CoreTriggerItem run(ExecutionContext ctx) {
        if (bodyFirst == null) {
            return nextAfter;
        }
        return condition.check(ctx) ? bodyFirst : nextAfter;
    }
}
