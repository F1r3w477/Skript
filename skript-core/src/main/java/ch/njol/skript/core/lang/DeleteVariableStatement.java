package ch.njol.skript.core.lang;

import ch.njol.skript.core.variables.VariableRef;

/**
 * Effect: delete %variable% — removes the variable from the current scope.
 */
public final class DeleteVariableStatement implements Statement {

    private final Object variableRef;

    public DeleteVariableStatement(Object variableRef) {
        this.variableRef = variableRef;
    }

    @Override
    public void run(ExecutionContext ctx) {
        if (variableRef instanceof VariableRef ref) {
            ctx.getVariableScope().remove(ref.getName());
        }
    }
}
