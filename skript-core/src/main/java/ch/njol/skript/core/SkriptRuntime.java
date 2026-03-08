package ch.njol.skript.core;

import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.trigger.CoreTriggerItem;
import ch.njol.skript.core.model.ScriptEventHandler;
import ch.njol.skript.core.model.ScriptFile;
import ch.njol.skript.core.variables.CoreVariables;
import ch.njol.skript.core.variables.VariableScope;
import ch.njol.skript.platform.SkriptCommandSender;
import ch.njol.skript.platform.SkriptLogger;
import ch.njol.skript.platform.SkriptPlatform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Dispatches named events to registered handlers and interprets a small set of effects
 * (e.g. broadcast, log, assert in test mode). Handlers are registered by
 * {@link SkriptEngine} after parsing. Events are delivered via
 * {@link SkriptBootstrap#fireEvent(String, RuntimeEventContext)}. Used internally by
 * {@link SkriptEngine}; platforms only trigger events through the bootstrap.
 */
final class SkriptRuntime {

    private final SkriptLogger logger;
    private final Map<String, List<ScriptEventHandler>> handlersByEvent = new HashMap<>();
    private final Map<ScriptEventHandler, String> testNameByHandler = new HashMap<>();

    SkriptRuntime(SkriptLogger logger) {
        this.logger = logger;
    }

    void registerScripts(List<ScriptFile> scripts) {
        registerScripts(scripts, null);
    }

    /**
     * Register script handlers. When platform is non-null, command sections
     * (eventName "command:name") are registered with the platform instead of the event map.
     */
    void registerScripts(List<ScriptFile> scripts, SkriptPlatform platform) {
        handlersByEvent.clear();
        testNameByHandler.clear();

        for (ScriptFile script : scripts) {
            List<String> testNames = script.getTestNames();
            String testNameForFile = testNames.size() == 1 ? testNames.get(0) : null;

            for (ScriptEventHandler handler : script.getEventHandlers()) {
                String eventName = handler.getEventName();
                if (platform != null && eventName.startsWith("command:")) {
                    String cmdName = eventName.substring("command:".length());
                    platform.registerCommand(cmdName, "Script command", (SkriptCommandSender sender, String[] args) -> {
                        RuntimeEventContext ctx = new RuntimeEventContext("command", sender.getName(), null, null);
                        executeHandler(handler, ctx);
                    });
                    continue;
                }
                String key = normaliseEventName(eventName);
                handlersByEvent
                    .computeIfAbsent(key, k -> new ArrayList<>())
                    .add(handler);

                String name = handler.getAssociatedTestName();
                if (name != null) {
                    testNameByHandler.put(handler, name);
                } else if (testNameForFile != null) {
                    testNameByHandler.put(handler, testNameForFile);
                }
            }
        }

        logger.info("Registered handlers for events: " + String.join(
            ", ", handlersByEvent.keySet()
        ));
    }

    void fireEvent(String eventName, RuntimeEventContext context) {
        String key = normaliseEventName(eventName);
        List<ScriptEventHandler> handlers = handlersByEvent.get(key);
        if (handlers == null || handlers.isEmpty()) {
            return;
        }

        String includeTest = CoreTestMode.INCLUDE_TEST;
        List<ScriptEventHandler> toRun = handlers;
        if (includeTest != null && !includeTest.isBlank() && "tests".equals(key)) {
            toRun = handlers.stream()
                .filter(h -> includeTest.equals(testNameByHandler.get(h)))
                .toList();
            logger.info("Dispatching event 'tests' (single-test filter='" + includeTest + "'): " + toRun.size() + " of " + handlers.size() + " handler(s).");
        } else {
            logger.info("Dispatching event '" + key + "' to " + handlers.size() + " handler(s)." +
                (context != null && context.getSubject() != null ? " subject=" + context.getSubject() : ""));
        }

        for (ScriptEventHandler handler : toRun) {
            executeHandler(handler, context);
        }
    }

    private void executeHandler(ScriptEventHandler handler, RuntimeEventContext context) {
        String testName = testNameByHandler.get(handler);
        VariableScope scope = new VariableScope();
        ExecutionContext ctx = new ExecutionContext(logger, context, scope, handler, testName);
        CoreVariables.setScope(scope);
        try {
            CoreTriggerItem start = handler.getFirstTriggerItem();
            if (start != null) {
                CoreTriggerItem.walk(start, ctx);
            }
        } finally {
            CoreVariables.clearScope();
        }
    }

    private static String normaliseEventName(String name) {
        return name.toLowerCase(Locale.ROOT).trim();
    }
}

