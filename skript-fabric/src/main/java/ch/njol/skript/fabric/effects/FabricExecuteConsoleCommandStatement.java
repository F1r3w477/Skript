package ch.njol.skript.fabric.effects;

import ch.njol.skript.core.event.EventValues;
import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.Statement;
import ch.njol.skript.core.SkriptBootstrap;
import ch.njol.skript.fabric.platform.FabricSkriptCommandSender;
import ch.njol.skript.fabric.platform.FabricSkriptPlatform;
import ch.njol.skript.platform.SkriptCommandExecutor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

/**
 * Fabric implementation of "execute console command %string%" so script-defined
 * commands (e.g. from "on command /name:") can be run from scripts.
 * Runs script commands directly via their executor when present; otherwise
 * dispatches through the server command manager.
 */
public final class FabricExecuteConsoleCommandStatement implements Statement {

    private final Object commandExpr;

    public FabricExecuteConsoleCommandStatement(Object commandExpr) {
        this.commandExpr = commandExpr;
    }

    @Override
    public void run(ExecutionContext ctx) {
        Object val = EventValues.resolve(commandExpr, ctx);
        String command = val != null ? String.valueOf(val).trim() : "";
        if (command.isEmpty()) return;

        var platform = SkriptBootstrap.getPlatform();
        if (!(platform instanceof FabricSkriptPlatform fabric)) return;
        MinecraftServer server = fabric.getServer();
        if (server == null) return;

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // Run script-defined commands directly so they work even if Brigadier registration timing differs
        int space = command.indexOf(' ');
        String name = space >= 0 ? command.substring(0, space) : command;
        String argsStr = space >= 0 ? command.substring(space + 1).trim() : "";
        String[] args = argsStr.isEmpty() ? new String[0] : argsStr.split("\\s+");

        SkriptCommandExecutor executor = fabric.getScriptCommands().get(name.toLowerCase());
        if (executor != null) {
            CommandSourceStack source = server.createCommandSourceStack();
            FabricSkriptCommandSender sender = new FabricSkriptCommandSender(source);
            executor.execute(sender, args);
            return;
        }

        CommandSourceStack source = server.createCommandSourceStack();
        server.getCommands().performPrefixedCommand(source, command);
    }
}
