package ch.njol.skript.core.lang;

/**
 * Statement that does nothing at runtime. Used when a line is recognised
 * (so we do not log "Unrecognised line") but no specific implementation
 * is registered yet.
 */
public final class NoOpStatement implements Statement {

    @Override
    public void run(ExecutionContext ctx) {
        // no-op
    }
}
