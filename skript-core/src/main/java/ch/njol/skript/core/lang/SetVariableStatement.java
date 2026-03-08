package ch.njol.skript.core.lang;

import ch.njol.skript.core.variables.VariableRef;

/**
 * Effect: set %variable% to %object%. Uses execution-scoped variable storage.
 */
public final class SetVariableStatement implements Statement {

    private final Object variableRef;
    private final Expression<Object> valueExpr;

    /**
     * @param variableRef parsed variable (e.g. {@link VariableRef} from "{_x}")
     * @param valueExpr    expression for the value (evaluated at runtime)
     */
    public SetVariableStatement(Object variableRef, Expression<Object> valueExpr) {
        this.variableRef = variableRef;
        this.valueExpr = valueExpr;
    }

    @Override
    public void run(ExecutionContext ctx) {
        if (!(variableRef instanceof VariableRef ref)) return;
        Object value = valueExpr != null ? valueExpr.get(ctx) : null;
        var scope = ctx.getVariableScope();
        if (value == null) {
            scope.remove(ref.getName());
        } else {
            scope.set(ref.getName(), value);
        }
    }
}
