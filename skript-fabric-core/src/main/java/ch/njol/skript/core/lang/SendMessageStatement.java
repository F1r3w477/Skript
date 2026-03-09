package ch.njol.skript.core.lang;

/**
 * Effect: send a message to the event player (if any), otherwise no-op.
 * Uses {@link ch.njol.skript.platform.SkriptPlatform#sendMessage(ch.njol.skript.platform.SkriptPlayerInfo, String)}.
 */
public final class SendMessageStatement implements Statement {

    private final String message;

    public SendMessageStatement(String message) {
        this.message = message;
    }

    @Override
    public void run(ExecutionContext ctx) {
        var player = ctx.getEventContext() != null ? ctx.getEventContext().getPlayer() : null;
        var platform = ctx.getPlatform();
        if (platform != null) {
            platform.sendMessage(player, message != null ? message : "");
        }
    }
}
