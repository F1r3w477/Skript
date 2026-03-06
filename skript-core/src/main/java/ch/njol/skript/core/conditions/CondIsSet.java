package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.variables.VariableRef;

/**
 * Condition: %variable% is [not] set. Checks whether the variable has a value in the current scope.
 */
public final class CondIsSet implements Condition {

    private final VariableRef variable;
    private final boolean negated;

    public CondIsSet(VariableRef variable) {
        this(variable, false);
    }

    public CondIsSet(VariableRef variable, boolean negated) {
        this.variable = variable;
        this.negated = negated;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        boolean set = ctx.getVariableScope().has(variable.getName());
        return negated ? !set : set;
    }
}
