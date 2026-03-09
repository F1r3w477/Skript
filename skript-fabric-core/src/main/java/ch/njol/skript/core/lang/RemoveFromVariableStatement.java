package ch.njol.skript.core.lang;

import ch.njol.skript.core.event.EventValues;
import ch.njol.skript.core.variables.VariableRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Effect: remove %object% from %variable%. Removes matching items from the variable's list.
 */
public final class RemoveFromVariableStatement implements Statement {

    private final VariableRef variable;
    private final Expression<Object> valueExpr;

    public RemoveFromVariableStatement(VariableRef variable, Expression<Object> valueExpr) {
        this.variable = variable;
        this.valueExpr = valueExpr;
    }

    @Override
    public void run(ExecutionContext ctx) {
        Object valueRaw = valueExpr != null ? valueExpr.get(ctx) : null;
        Object value = EventValues.resolve(valueRaw, ctx);
        Object current = ctx.getVariableScope().get(variable.getName());
        if (current == null) return;
        List<Object> list;
        if (current instanceof List<?> l) {
            list = new ArrayList<>(l);
        } else if (current instanceof Collection<?> c) {
            list = new ArrayList<>(c);
        } else if (current instanceof Object[] a) {
            list = new ArrayList<>(List.of(a));
        } else {
            list = new ArrayList<>();
            list.add(current);
        }
        if (value instanceof Collection<?> c) {
            for (Object e : c) {
                list.removeIf(v -> equalsForRemove(v, e));
            }
        } else if (value instanceof Object[] a) {
            for (Object e : a) {
                list.removeIf(v -> equalsForRemove(v, e));
            }
        } else if (value instanceof Iterable<?> it) {
            for (Object e : it) {
                list.removeIf(v -> equalsForRemove(v, e));
            }
        } else {
            list.removeIf(v -> equalsForRemove(v, value));
        }
        ctx.getVariableScope().set(variable.getName(), list.isEmpty() ? null : list);
    }

    private static boolean equalsForRemove(Object a, Object b) {
        if (Objects.equals(a, b)) return true;
        if (a instanceof Number na && b instanceof Number nb) {
            return na.doubleValue() == nb.doubleValue();
        }
        return false;
    }
}
