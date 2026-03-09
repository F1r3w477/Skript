package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;

import java.util.Collection;
import java.util.Objects;

/**
 * Condition: %string% contains %string% or %object% is in %objects%.
 * Uses the expression layer for runtime evaluation.
 */
public final class CondContains implements Condition {

    private final Expression<Object> haystackExpr;
    private final Expression<Object> needleExpr;
    private final boolean negated;

    public CondContains(Expression<Object> haystackExpr, Expression<Object> needleExpr) {
        this(haystackExpr, needleExpr, false);
    }

    public CondContains(Expression<Object> haystackExpr, Expression<Object> needleExpr, boolean negated) {
        this.haystackExpr = haystackExpr;
        this.needleExpr = needleExpr;
        this.negated = negated;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        Object haystack = haystackExpr != null ? haystackExpr.get(ctx) : null;
        Object needle = needleExpr != null ? needleExpr.get(ctx) : null;
        boolean contains = contains(haystack, needle);
        return negated ? !contains : contains;
    }

    private static boolean contains(Object haystack, Object needle) {
        if (needle == null) {
            return haystack == null;
        }
        if (haystack instanceof String) {
            return ((String) haystack).contains(String.valueOf(needle));
        }
        if (haystack instanceof Collection<?>) {
            return ((Collection<?>) haystack).stream().anyMatch(e -> Objects.equals(e, needle));
        }
        if (haystack instanceof Object[]) {
            for (Object e : (Object[]) haystack) {
                if (Objects.equals(e, needle)) return true;
            }
            return false;
        }
        return false;
    }
}
