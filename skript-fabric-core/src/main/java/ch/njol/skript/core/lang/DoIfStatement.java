package ch.njol.skript.core.lang;

import ch.njol.skript.core.condition.Condition;

/**
 * Runs an effect only when a condition is true. Used for "set x to y if condition".
 */
public final class DoIfStatement implements Statement {

    private final Condition condition;
    private final Statement inner;

    public DoIfStatement(Condition condition, Statement inner) {
        this.condition = condition;
        this.inner = inner;
    }

    @Override
    public void run(ExecutionContext ctx) {
        boolean trace = Boolean.getBoolean("skript.fabric.trace");
        if (condition == null) {
            if (trace) ctx.getLogger().info("[FabricTrace] DoIfStatement.run: condition is null, skipping");
            return;
        }
        boolean pass = condition.check(ctx);
        if (trace) {
            ctx.getLogger().info("[FabricTrace] DoIfStatement.run: condition.check=" + pass + ", inner=" + (inner != null));
        }
        if (pass && inner != null) {
            inner.run(ctx);
        }
    }
}
