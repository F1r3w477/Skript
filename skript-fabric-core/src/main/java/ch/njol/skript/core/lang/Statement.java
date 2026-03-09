package ch.njol.skript.core.lang;

import ch.njol.skript.core.RuntimeEventContext;

/**
 * Platform-agnostic statement (effect) that can be executed by the core runtime.
 * Implementations are created by the parser and run in order when an event fires.
 */
public interface Statement {

    /**
     * Execute this statement. May log, update test state, or do nothing.
     *
     * @param ctx execution context (logger, event context, optional test name)
     */
    void run(ExecutionContext ctx);
}
