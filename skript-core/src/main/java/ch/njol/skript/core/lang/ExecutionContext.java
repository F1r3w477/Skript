package ch.njol.skript.core.lang;

import ch.njol.skript.core.RuntimeEventContext;
import ch.njol.skript.core.SkriptBootstrap;
import ch.njol.skript.core.model.ScriptEventHandler;
import ch.njol.skript.core.variables.VariableScope;
import ch.njol.skript.platform.SkriptLogger;
import ch.njol.skript.platform.SkriptPlatform;

/**
 * Context passed to {@link Statement#run(ExecutionContext)}.
 * Provides logger, event context, variable scope, platform, and optional test name for the current handler.
 */
public final class ExecutionContext {

    private final SkriptLogger logger;
    private final RuntimeEventContext eventContext;
    private final VariableScope variableScope;
    private final ScriptEventHandler handler;
    private final String testName;

    public ExecutionContext(SkriptLogger logger, RuntimeEventContext eventContext,
                            VariableScope variableScope, ScriptEventHandler handler, String testName) {
        this.logger = logger;
        this.eventContext = eventContext;
        this.variableScope = variableScope != null ? variableScope : new VariableScope();
        this.handler = handler;
        this.testName = testName;
    }

    public SkriptLogger getLogger() {
        return logger;
    }

    public RuntimeEventContext getEventContext() {
        return eventContext;
    }

    /**
     * Execution-scoped variable storage for this trigger run (e.g. {_x}).
     */
    public VariableScope getVariableScope() {
        return variableScope;
    }

    public ScriptEventHandler getHandler() {
        return handler;
    }

    /**
     * Test name associated with this handler (if exactly one test was declared in the script file), else null.
     */
    public String getTestName() {
        return testName;
    }

    /**
     * Platform (Bukkit, Fabric, etc.) for conditions/effects that need platform hooks.
     */
    public SkriptPlatform getPlatform() {
        return SkriptBootstrap.getPlatform();
    }
}
