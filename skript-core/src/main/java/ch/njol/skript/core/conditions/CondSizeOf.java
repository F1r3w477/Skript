package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;
import ch.njol.skript.core.variables.VariableRef;

import java.util.Collection;

/**
 * Condition: size of %variable% is %number% / size of %variable% = %number%.
 * Resolves the variable at runtime and compares its size (list/collection/string length) to the number.
 */
public final class CondSizeOf implements Condition {

    private final VariableRef variable;
    private final Expression<Object> expectedSize;

    public CondSizeOf(VariableRef variable, Expression<Object> expectedSize) {
        this.variable = variable;
        this.expectedSize = expectedSize;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        Object val = ctx != null && ctx.getVariableScope() != null
            ? ctx.getVariableScope().get(variable.getName())
            : null;
        Object expObj = expectedSize != null ? expectedSize.get(ctx) : null;
        int expected = expObj instanceof Number n ? n.intValue() : 0;
        int actual = sizeOf(val);
        return actual == expected;
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
