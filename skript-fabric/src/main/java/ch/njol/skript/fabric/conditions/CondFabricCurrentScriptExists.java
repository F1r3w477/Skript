package ch.njol.skript.fabric.conditions;

import ch.njol.skript.core.condition.Condition;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.fabric.util.FabricLoadedScripts;

/**
 * Condition "the current script exists" for script reflection tests.
 */
public final class CondFabricCurrentScriptExists implements Condition {

    @Override
    public boolean check(ExecutionContext ctx) {
        if (ctx == null) return false;
        if (ctx.getCurrentScript() != null) return true;
        return FabricLoadedScripts.getScriptForHandler(ctx.getHandler()) != null;
    }
}
