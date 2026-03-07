package ch.njol.skript.core.lang.trigger;

import ch.njol.skript.core.lang.ExecutionContext;

/**
 * Runs a body chain a fixed number of times, then continues to nextAfter.
 * Uses ExecutionContext temporary state for the remaining count so the same
 * trigger can run again in a subsequent handler (e.g. another test).
 */
public final class LoopNTimesTriggerItem extends CoreTriggerItem {

    private static final String KEY_PREFIX = "loop_ntimes_";
    private static int nextId = 0;

    private final String stateKey;
    private final int times;
    private final CoreTriggerItem nextAfter;
    private CoreTriggerItem bodyFirst;

    public LoopNTimesTriggerItem(int times, CoreTriggerItem nextAfter) {
        this.times = Math.max(0, times);
        this.nextAfter = nextAfter;
        this.stateKey = KEY_PREFIX + (nextId++);
    }

    public void setBodyFirst(CoreTriggerItem bodyFirst) {
        this.bodyFirst = bodyFirst;
    }

    @Override
    public CoreTriggerItem run(ExecutionContext ctx) {
        if (bodyFirst == null || times <= 0) {
            return nextAfter;
        }
        Integer remaining = ctx.getTemporary(stateKey);
        if (remaining == null) {
            ctx.setTemporary(stateKey, times);
            remaining = times;
        }
        if (remaining <= 0) {
            ctx.setTemporary(stateKey, null);
            return nextAfter;
        }
        return bodyFirst;
    }

    /**
     * Called by the tail item after each body run. Decrements counter and returns
     * bodyFirst to loop again or nextAfter to exit.
     */
    public CoreTriggerItem afterBodyRun(ExecutionContext ctx) {
        Integer remaining = ctx.getTemporary(stateKey);
        if (remaining == null) return nextAfter;
        remaining--;
        if (remaining <= 0) {
            ctx.setTemporary(stateKey, null);
            return nextAfter;
        }
        ctx.setTemporary(stateKey, remaining);
        return bodyFirst;
    }

    /**
     * Tail trigger item that follows the loop body; decrements the loop counter and
     * returns the next item (either loop again or exit).
     */
    public static final class Tail extends CoreTriggerItem {
        private final LoopNTimesTriggerItem parent;

        public Tail(LoopNTimesTriggerItem parent) {
            this.parent = parent;
        }

        @Override
        public CoreTriggerItem run(ExecutionContext ctx) {
            return parent.afterBodyRun(ctx);
        }
    }
}
