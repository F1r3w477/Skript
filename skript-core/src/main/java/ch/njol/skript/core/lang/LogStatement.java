package ch.njol.skript.core.lang;

/**
 * Effect: log a message (logged by the platform).
 */
public final class LogStatement implements Statement {

    private final String message;

    public LogStatement(String message) {
        this.message = message;
    }

    @Override
    public void run(ExecutionContext ctx) {
        ctx.getLogger().info("[log] " + (message != null ? message : ""));
    }
}
