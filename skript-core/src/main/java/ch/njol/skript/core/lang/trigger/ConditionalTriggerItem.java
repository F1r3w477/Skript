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

    public ConditionalTriggerItem(Condition condition, CoreTriggerItem thenItem, CoreTriggerItem elseItem) {
        this.condition = condition;
        this.thenItem = thenItem;
        this.elseItem = elseItem;
    }

    @Override
    public CoreTriggerItem run(ExecutionContext ctx) {
        return condition.check(ctx) ? thenItem : elseItem;
    }
}
