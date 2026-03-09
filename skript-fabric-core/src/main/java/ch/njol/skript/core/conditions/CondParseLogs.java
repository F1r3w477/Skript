package ch.njol.skript.core.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;
import ch.njol.skript.core.parse.ParseLogsHolder;

/**
 * Conditions for last parse logs: is set, is not set, contain, does not contain.
 */
public final class CondParseLogs implements Condition {

    public enum Kind { IS_SET, IS_NOT_SET, CONTAINS, DOES_NOT_CONTAIN }

    private final Kind kind;
    private final Expression<Object> stringExpr;

    public CondParseLogs(Kind kind, Expression<Object> stringExpr) {
        this.kind = kind;
        this.stringExpr = stringExpr;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        String logs = ParseLogsHolder.get();
        if (Boolean.getBoolean("skript.fabric.trace")) {
            ctx.getLogger().info("[FabricTrace] CondParseLogs.check: kind=" + kind + ", logs=" + (logs != null ? "\"" + logs + "\"" : "null"));
        }
        switch (kind) {
            case IS_SET:
                return logs != null && !logs.isEmpty();
            case IS_NOT_SET:
                return logs == null || logs.isEmpty();
            case CONTAINS: {
                Object needle = stringExpr != null ? stringExpr.get(ctx) : null;
                String s = needle != null ? String.valueOf(needle) : "";
                return logs != null && logs.contains(s);
            }
            case DOES_NOT_CONTAIN: {
                Object needle = stringExpr != null ? stringExpr.get(ctx) : null;
                String s = needle != null ? String.valueOf(needle) : "";
                return logs == null || !logs.contains(s);
            }
            default:
                return false;
        }
    }
}
