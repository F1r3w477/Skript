package ch.njol.skript.core.conditions;

import ch.njol.skript.core.SkriptBootstrap;
import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;

/**
 * Condition: plugin %string% is enabled. Uses platform to check at runtime (and parse time for parse-if).
 */
public final class CondPluginEnabled implements Condition {

    private final Expression<Object> pluginNameExpr;

    public CondPluginEnabled(Expression<Object> pluginNameExpr) {
        this.pluginNameExpr = pluginNameExpr;
    }

    @Override
    public boolean check(ExecutionContext ctx) {
        Object val = pluginNameExpr != null ? pluginNameExpr.get(ctx) : null;
        String name = val != null ? val.toString() : null;
        var platform = SkriptBootstrap.getPlatform();
        return platform != null && platform.isPluginEnabled(name);
    }
}
