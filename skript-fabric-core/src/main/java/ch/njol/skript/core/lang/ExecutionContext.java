package ch.njol.skript.core.lang;

import ch.njol.skript.core.RuntimeEventContext;
import ch.njol.skript.core.SkriptBootstrap;
import ch.njol.skript.core.model.ScriptEventHandler;
import ch.njol.skript.core.model.ScriptFile;
import ch.njol.skript.core.variables.VariableScope;
import ch.njol.skript.platform.SkriptLogger;
import ch.njol.skript.platform.SkriptPlatform;

import java.util.HashMap;
import java.util.Map;

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
    private final ScriptFile currentScript;
    private Map<String, Object> temporary;

    public ExecutionContext(SkriptLogger logger, RuntimeEventContext eventContext,
                            VariableScope variableScope, ScriptEventHandler handler, String testName) {
        this(logger, eventContext, variableScope, handler, testName, null);
    }

    public ExecutionContext(SkriptLogger logger, RuntimeEventContext eventContext,
                            VariableScope variableScope, ScriptEventHandler handler, String testName,
                            ScriptFile currentScript) {
        this.logger = logger;
        this.eventContext = eventContext;
        this.variableScope = variableScope != null ? variableScope : new VariableScope();
        this.handler = handler;
        this.testName = testName;
        this.currentScript = currentScript;
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
     * The script file that contains the handler currently running (for "the current script"). Null if unknown.
     */
    public ScriptFile getCurrentScript() {
        return currentScript;
    }

    /**
     * Path of the current script as a string (forward slashes), or null if unknown.
     */
    public String getCurrentScriptPath() {
        return currentScript != null ? currentScript.getPath().toString().replace('\\', '/') : null;
    }

    /**
     * Platform (Bukkit, Fabric, etc.) for conditions/effects that need platform hooks.
     */
    public SkriptPlatform getPlatform() {
        return SkriptBootstrap.getPlatform();
    }

    /**
     * Per-run temporary state (e.g. loop counters). Cleared when the handler run ends.
     */
    @SuppressWarnings("unchecked")
    public <T> T getTemporary(String key) {
        return temporary != null ? (T) temporary.get(key) : null;
    }

    /**
     * Set per-run temporary state.
     */
    public void setTemporary(String key, Object value) {
        if (temporary == null) temporary = new HashMap<>();
        if (value == null) temporary.remove(key);
        else temporary.put(key, value);
    }
}
