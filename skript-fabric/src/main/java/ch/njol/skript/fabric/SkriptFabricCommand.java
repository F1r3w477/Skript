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

/**
 * Registers the /skript command on Fabric and forwards execution to the platform's executor.
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
