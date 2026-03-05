package ch.njol.skript.core.lang;

import ch.njol.skript.core.model.ScriptEventHandler;

/**
 * Fallback for a line that could not be parsed as a known statement.
 * Logs a warning with the handler name and raw line.
 */
public final class UnrecognisedStatement implements Statement {

    private final String rawLine;

    public UnrecognisedStatement(String rawLine) {
        this.rawLine = rawLine;
    }

    @Override
    public void run(ExecutionContext ctx) {
        ctx.getLogger().warn("[runtime] Unrecognised line in handler '" + ctx.getHandler().getEventName() + "': " + rawLine);
    }
}
