package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;
import ch.njol.skript.core.variables.VariableRef;

import java.util.Collection;

/**
 * Condition: size of %variable% > %number%, size of %variable% < %number%, etc.
 */
public final class CondSizeOfCompare implements Condition {

    public enum Op { GREATER, LESS }

    private final VariableRef variable;
    private final Expression<Object> compareTo;
    private final Op op;

    public CondSizeOfCompare(VariableRef variable, Expression<Object> compareTo, Op op) {
        this.variable = variable;
        this.compareTo = compareTo;
        this.op = op;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        Object val = ctx != null && ctx.getVariableScope() != null
            ? ctx.getVariableScope().get(variable.getName())
            : null;
        Object expObj = compareTo != null ? compareTo.get(ctx) : null;
        int expected = expObj instanceof Number n ? n.intValue() : 0;
        int actual = sizeOf(val);
        return op == Op.GREATER ? actual > expected : actual < expected;
    }

    private static int sizeOf(Object o) {
        if (o == null) return 0;
        if (o instanceof Collection<?> c) return c.size();
        if (o instanceof Object[] a) return a.length;
        if (o instanceof CharSequence s) return s.length();
        if (o instanceof Number n) return n.intValue();
        return 1;
    }
}
