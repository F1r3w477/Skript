package ch.njol.skript.core.lang;

import ch.njol.skript.core.condition.Condition;

/**
 * Expression that evaluates a condition and returns one of two expressions.
 * Used for "A if condition otherwise B" / "A if condition else B".
 */
public final class ExprTernary implements Expression<Object> {

    private final Condition condition;
    private final Expression<Object> thenExpr;
    private final Expression<Object> elseExpr;

    public ExprTernary(Condition condition, Expression<Object> thenExpr, Expression<Object> elseExpr) {
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    @Override
    public Object get(ExecutionContext ctx) {
        if (condition == null) return elseExpr != null ? elseExpr.get(ctx) : null;
        return condition.check(ctx) ? (thenExpr != null ? thenExpr.get(ctx) : null) : (elseExpr != null ? elseExpr.get(ctx) : null);
    }
}
