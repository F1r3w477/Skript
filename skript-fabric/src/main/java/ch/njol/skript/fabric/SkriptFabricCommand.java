package ch.njol.skript.fabric;

import ch.njol.skript.core.SkriptBootstrap;
import ch.njol.skript.fabric.platform.FabricSkriptCommandSender;
import ch.njol.skript.fabric.platform.FabricSkriptPlatform;
import ch.njol.skript.platform.SkriptCommandExecutor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands.CommandSelection;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.Map;

/**
 * Registers the /skript command and all script-defined commands (e.g. "on command /testing1:")
 * on Fabric, forwarding execution to the platform executors.
 */
final class SkriptFabricCommand {

    static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                        CommandBuildContext registryAccess,
                        CommandSelection environment) {
        dispatcher.register(
            Commands.literal("skript")
                .then(Commands.literal("reload")
                    .executes(ctx -> runSubcommand(ctx, "reload")))
                .then(Commands.literal("test")
                    .executes(ctx -> runSubcommand(ctx, "test")))
                .executes(ctx -> {
                    FabricSkriptCommandSender sender = new FabricSkriptCommandSender(ctx.getSource());
                    sender.sendMessage("Skript (Fabric). Usage: /skript reload | /skript test");
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                })
        );

        // Register script commands from "on command /name:" so they are executable
        var platform = SkriptBootstrap.getPlatform();
        if (platform instanceof FabricSkriptPlatform fabric) {
            for (Map.Entry<String, SkriptCommandExecutor> e : fabric.getScriptCommands().entrySet()) {
                String name = e.getKey();
                SkriptCommandExecutor executor = e.getValue();
                dispatcher.register(
                    Commands.literal(name)
                        .executes(ctx -> runScriptCommand(ctx, name, executor, new String[0]))
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                            .executes(ctx -> runScriptCommand(ctx, name, executor, splitArgs(ctx.getArgument("args", String.class)))))
                );
            }
        }
    }

    private static int runScriptCommand(CommandContext<CommandSourceStack> ctx, String name, SkriptCommandExecutor executor, String[] args) {
        FabricSkriptCommandSender sender = new FabricSkriptCommandSender(ctx.getSource());
        executor.execute(sender, args);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static String[] splitArgs(String s) {
        if (s == null || s.isBlank()) return new String[0];
        return s.trim().split("\\s+");
    }

    private static int runSubcommand(CommandContext<CommandSourceStack> ctx, String sub) {
        SkriptCommandExecutor executor = getExecutor();
        if (executor == null) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Skript command not ready."));
            return 0;
        }
        FabricSkriptCommandSender sender = new FabricSkriptCommandSender(ctx.getSource());
        executor.execute(sender, new String[] { sub });
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static SkriptCommandExecutor getExecutor() {
        if (SkriptBootstrap.getPlatform() instanceof FabricSkriptPlatform fabric) {
            return fabric.getSkriptCommandExecutor();
        }
        return null;
    }
}
