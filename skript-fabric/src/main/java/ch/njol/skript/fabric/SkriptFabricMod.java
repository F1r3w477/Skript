package ch.njol.skript.fabric;

import ch.njol.skript.core.RuntimeEventContext;
import ch.njol.skript.core.SkriptBootstrap;
import ch.njol.skript.core.CoreTestMode;
import ch.njol.skript.core.TestRegistry;
import ch.njol.skript.fabric.platform.FabricSkriptLogger;
import ch.njol.skript.fabric.platform.FabricSkriptPlatform;
import ch.njol.skript.fabric.testing.FabricTestResults;
import ch.njol.skript.platform.SkriptCommandSender;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public final class SkriptFabricMod implements ModInitializer {

    public static final String MOD_ID = "skript-fabric";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Skript Fabric port.");

        FabricSkriptPlatform platform = new FabricSkriptPlatform(new FabricSkriptLogger(LOGGER));
        SkriptBootstrap.start(platform);

        platform.registerCommand("skript", "Skript commands", SkriptFabricMod::handleSkriptCommand);

        CommandRegistrationCallback.EVENT.register(SkriptFabricCommand::register);

        // Bridge a minimal set of Fabric events into the shared runtime.
        new FabricEventBridge();

        // When running under the upstream Skript test harness, defer test execution
        // until after the server has started so that "load" has fired and the
        // platform has a server reference (getOnlinePlayers(), etc.).
        if (Boolean.getBoolean("skript.testing.enabled")) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                SkriptBootstrap.fireEvent("tests", null);
                FabricTestResults.maybeWriteInitialResultsAndExit(LOGGER);
            });
        }
    }

    private static void handleSkriptCommand(SkriptCommandSender sender, String[] args) {
        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0])) {
            SkriptBootstrap.reloadScripts();
            SkriptBootstrap.fireEvent("load", new RuntimeEventContext("script_reload", null));
            SkriptBootstrap.fireEvent("script load", null);
            sender.sendMessage("Scripts reloaded.");
            return;
        }
        if (args.length >= 1 && "test".equalsIgnoreCase(args[0])) {
            if (CoreTestMode.ENABLED) {
                SkriptBootstrap.fireEvent("tests", null);
                var succeeded = TestRegistry.getSucceededTests();
                var failed = TestRegistry.getFailedTests();
                sender.sendMessage("Tests run: " + succeeded.size() + " passed, " + failed.size() + " failed.");
                for (Map.Entry<String, String> e : failed.entrySet()) {
                    sender.sendMessage("  FAIL " + e.getKey() + ": " + e.getValue());
                }
            } else {
                sender.sendMessage("Test mode is not enabled (use test harness to run tests).");
            }
            return;
        }
        sender.sendMessage("Usage: /skript reload | /skript test");
    }
}

