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
        if (condition != null && condition.check(ctx) && inner != null) {
            inner.run(ctx);
        }
    }
}
