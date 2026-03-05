package ch.njol.skript.fabric.platform;

import ch.njol.skript.platform.SkriptCommandSender;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric adapter: wraps a {@link CommandSourceStack} as {@link SkriptCommandSender}.
 */
public final class FabricSkriptCommandSender implements SkriptCommandSender {

    private final CommandSourceStack source;

    public FabricSkriptCommandSender(CommandSourceStack source) {
        this.source = source;
    }

    @Override
    public String getName() {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getName().getString();
        }
        return "Console";
    }

    @Override
    public void sendMessage(String message) {
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(message), false);
    }

    @Override
    public boolean isConsole() {
        return !(source.getEntity() instanceof ServerPlayer);
    }
}
