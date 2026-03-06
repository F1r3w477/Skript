package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;

import java.util.Objects;

/**
 * Condition: %object% is/equals %object%. Compares two parsed values at runtime.
 */
public final class CondCompare implements Condition {

    private final Object left;
    private final Object right;

    public CondCompare(Object left, Object right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        return Objects.equals(left, right);
    }
}
