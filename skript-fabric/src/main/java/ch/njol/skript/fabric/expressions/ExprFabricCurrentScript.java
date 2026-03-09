package ch.njol.skript.fabric.expressions;

import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;
import ch.njol.skript.core.model.ScriptFile;
import ch.njol.skript.fabric.util.FabricLoadedScripts;

/**
 * Expression for "the current script" - returns the path string of the script
 * that contains the currently running handler (from context when available, else handler lookup).
 */
public final class ExprFabricCurrentScript implements Expression<Object> {

    @Override
    public Object get(ExecutionContext ctx) {
        if (ctx == null) return null;
        String path = ctx.getCurrentScriptPath();
        if (path != null) return path;
        ScriptFile script = FabricLoadedScripts.getScriptForHandler(ctx.getHandler());
        return script != null ? script.getPath().toString().replace('\\', '/') : null;
    }
}
