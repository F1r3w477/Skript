package ch.njol.skript.bukkit.platform;

import ch.njol.skript.platform.SkriptCommandSender;
import org.bukkit.command.CommandSender;

/**
 * Bukkit adapter: wraps a {@link CommandSender} as {@link SkriptCommandSender}.
 */
public final class BukkitSkriptCommandSender implements SkriptCommandSender {

    private final CommandSender sender;

    public BukkitSkriptCommandSender(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public String getName() {
        return sender.getName();
    }

    @Override
    public void sendMessage(String message) {
        sender.sendMessage(message);
    }

    @Override
    public boolean isConsole() {
        return !(sender instanceof org.bukkit.entity.Player);
    }
}
