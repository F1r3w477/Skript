package ch.njol.skript.core.lang.trigger;

import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Statement;

/**
 * Trigger item that runs a single {@link Statement} then continues to next.
 */
public final class StatementTriggerItem extends CoreTriggerItem {

    private final Statement statement;
    private final CoreTriggerItem next;

    public StatementTriggerItem(Statement statement, CoreTriggerItem next) {
        this.statement = statement;
        this.next = next;
    }

    public Statement getStatement() {
        return statement;
    }

    public CoreTriggerItem getNext() {
        return next;
    }

    @Override
    public CoreTriggerItem run(ExecutionContext ctx) {
        statement.run(ctx);
        return next;
    }
}
