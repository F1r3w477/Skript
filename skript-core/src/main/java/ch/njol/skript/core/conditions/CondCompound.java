package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;

import java.util.List;

/**
 * Combines multiple conditions with AND or OR. Used for multiline "if all" / "if any".
 */
public final class CondCompound implements Condition {

    public enum Operator {
        AND {
            @Override
            boolean combine(boolean a, boolean b) { return a && b; }
        },
        OR {
            @Override
            boolean combine(boolean a, boolean b) { return a || b; }
        };
        abstract boolean combine(boolean a, boolean b);
    }

    private final List<Condition> conditions;
    private final Operator operator;

    public CondCompound(List<Condition> conditions, Operator operator) {
        this.conditions = List.copyOf(conditions);
        this.operator = operator;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        if (conditions.isEmpty()) return operator == Operator.AND;
        boolean acc = conditions.get(0).check(ctx);
        for (int i = 1; i < conditions.size(); i++) {
            acc = operator.combine(acc, conditions.get(i).check(ctx));
        }
        return acc;
    }
}
