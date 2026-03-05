package ch.njol.skript.core.lang;

import ch.njol.skript.core.RuntimeEventContext;
import ch.njol.skript.core.model.ScriptEventHandler;
import ch.njol.skript.platform.SkriptLogger;

/**
 * Context passed to {@link Statement#run(ExecutionContext)}.
 * Provides logger, event context, and optional test name for the current handler.
 */
public final class ExecutionContext {

    private final SkriptLogger logger;
    private final RuntimeEventContext eventContext;
    private final ScriptEventHandler handler;
    private final String testName;

    public ExecutionContext(SkriptLogger logger, RuntimeEventContext eventContext,
                           ScriptEventHandler handler, String testName) {
        this.logger = logger;
        this.eventContext = eventContext;
        this.handler = handler;
        this.testName = testName;
    }

    public SkriptLogger getLogger() {
        return logger;
    }

    public RuntimeEventContext getEventContext() {
        return eventContext;
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
}
