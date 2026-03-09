package ch.njol.skript.core.lang;

/**
 * Effect: broadcast a message (logged by the platform).
 */
public final class BroadcastStatement implements Statement {

    private final String message;

    public BroadcastStatement(String message) {
        this.message = message;
    }

    @Override
    public void run(ExecutionContext ctx) {
        ctx.getLogger().info("[broadcast] " + (message != null ? message : ""));
    }
}
