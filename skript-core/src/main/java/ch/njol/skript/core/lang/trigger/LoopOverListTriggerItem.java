package ch.njol.skript.core.lang.trigger;

import ch.njol.skript.core.lang.ExecutionContext;

import java.util.List;

/**
 * Loops over a list (e.g. "loop 1, 2 and 3:"). Sets loop_value for each iteration.
 */
public final class LoopOverListTriggerItem extends CoreTriggerItem {

    private static final String STATE_KEY_PREFIX = "loop_over_";
    private static int nextId = 0;

    private final String stateKey;
    private final List<Object> elements;
    private final CoreTriggerItem nextAfter;
    private CoreTriggerItem bodyFirst;

    public LoopOverListTriggerItem(List<Object> elements, CoreTriggerItem nextAfter) {
        this.stateKey = STATE_KEY_PREFIX + (nextId++);
        this.elements = elements;
        this.nextAfter = nextAfter;
    }

    public void setBodyFirst(CoreTriggerItem bodyFirst) {
        this.bodyFirst = bodyFirst;
    }

    @Override
    public CoreTriggerItem run(ExecutionContext ctx) {
        if (bodyFirst == null || elements == null || elements.isEmpty()) {
            ctx.setTemporary("loop_value", null);
            return nextAfter;
        }
        Integer idx = ctx.getTemporary(stateKey);
        if (idx == null) idx = 0;
        if (idx >= elements.size()) {
            ctx.setTemporary(stateKey, null);
            ctx.setTemporary("loop_value", null);
            return nextAfter;
        }
        ctx.setTemporary("loop_value", elements.get(idx));
        ctx.setTemporary(stateKey, idx + 1);
        return bodyFirst;
    }

    public CoreTriggerItem afterBodyRun(ExecutionContext ctx) {
        return run(ctx);
    }

    public static final class Tail extends CoreTriggerItem {
        private final LoopOverListTriggerItem parent;

        public Tail(LoopOverListTriggerItem parent) {
            this.parent = parent;
        }

        @Override
        public CoreTriggerItem run(ExecutionContext ctx) {
            return parent.afterBodyRun(ctx);
        }
    }
}
