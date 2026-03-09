package ch.njol.skript.core.lang;

import ch.njol.skript.core.event.EventValues;
import ch.njol.skript.core.variables.VariableRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Effect: add %object% to %variable%. Appends to the variable's list or creates a new list.
 */
public final class AddToVariableStatement implements Statement {

    private final VariableRef variable;
    private final Expression<Object> valueExpr;

    public AddToVariableStatement(VariableRef variable, Expression<Object> valueExpr) {
        this.variable = variable;
        this.valueExpr = valueExpr;
    }

    @Override
    public void run(ExecutionContext ctx) {
        Object value = valueExpr != null ? valueExpr.get(ctx) : null;
        value = EventValues.resolve(value, ctx);
        Object current = ctx.getVariableScope().get(variable.getName());
        List<Object> list;
        if (current instanceof List<?> l) {
            list = new ArrayList<>(l);
        } else if (current instanceof Collection<?> c) {
            list = new ArrayList<>(c);
        } else if (current instanceof Object[] a) {
            list = new ArrayList<>(List.of(a));
        } else if (current != null) {
            list = new ArrayList<>();
            list.add(current);
        } else {
            list = new ArrayList<>();
        }
        if (value instanceof Collection<?> c) {
            list.addAll(c);
        } else if (value instanceof Object[] a) {
            list.addAll(List.of(a));
        } else if (value != null) {
            list.add(value);
        }
        ctx.getVariableScope().set(variable.getName(), list);
    }
}
