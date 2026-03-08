package ch.njol.skript.core.lang;

import ch.njol.skript.core.variables.VariableRef;

/**
 * Effect: clear %variable%. Removes the variable from scope.
 */
public final class ClearVariableStatement implements Statement {

    private final VariableRef variable;

    public ClearVariableStatement(VariableRef variable) {
        this.variable = variable;
    }

    @Override
    public void run(ExecutionContext ctx) {
        ctx.getVariableScope().remove(variable.getName());
    }
}
