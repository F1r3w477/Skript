package ch.njol.skript.fabric.effects;

import ch.njol.skript.core.event.EventValues;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.core.SkriptBootstrap;

/**
 * Fabric implementation of "load script named %variable%" and "reload script named %variable%"
 * so that tests (enable script, reload script) can run. Performs a full reload and fires
 * "script load" so handlers with "on script load:" run.
 */
public final class FabricLoadScriptStatement implements Statement {

    private final Object pathExpression;

    public FabricLoadScriptStatement(Object pathExpression) {
        this.pathExpression = pathExpression;
    }

    @Override
    public void run(ExecutionContext ctx) {
        // Path is only used for semantics; we do full reload since core has no single-script load.
        Object path = pathExpression != null ? EventValues.resolve(pathExpression, ctx) : null;
        SkriptBootstrap.reloadScripts();
        SkriptBootstrap.fireEvent("script load", null);
    }
}
