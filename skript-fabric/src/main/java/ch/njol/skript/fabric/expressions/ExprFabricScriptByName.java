package ch.njol.skript.fabric.expressions;

import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Expression;
import ch.njol.skript.fabric.util.FabricLoadedScripts;
import ch.njol.skript.core.model.ScriptFile;

/**
 * Expression that returns the path string of a script found by name/path, or null.
 * Used for "the script named %string%".
 */
public final class ExprFabricScriptByName implements Expression<Object> {

    private final Expression<Object> nameExpr;

    public ExprFabricScriptByName(Expression<Object> nameExpr) {
        this.nameExpr = nameExpr;
    }

    @Override
    public Object get(ExecutionContext ctx) {
        Object name = nameExpr != null ? nameExpr.get(ctx) : null;
        String pathOrName = name != null ? name.toString().trim() : null;
        if (pathOrName == null || pathOrName.isEmpty()) return null;
        ScriptFile script = FabricLoadedScripts.findScript(pathOrName);
        return script != null ? script.getPath().toString().replace('\\', '/') : null;
    }
}
