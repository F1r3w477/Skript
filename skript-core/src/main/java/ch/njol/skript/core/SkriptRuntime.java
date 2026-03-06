package ch.njol.skript.core;

import ch.njol.skript.core.lang.ExecutionContext;
import ch.njol.skript.core.lang.trigger.CoreTriggerItem;
import ch.njol.skript.core.model.ScriptEventHandler;
import ch.njol.skript.core.model.ScriptFile;
import ch.njol.skript.core.variables.CoreVariables;
import ch.njol.skript.core.variables.VariableScope;
import ch.njol.skript.platform.SkriptLogger;

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
        handlersByEvent.clear();
        testNameByHandler.clear();

        for (ScriptFile script : scripts) {
            List<String> testNames = script.getTestNames();
            String testNameForFile = testNames.size() == 1 ? testNames.get(0) : null;

            for (ScriptEventHandler handler : script.getEventHandlers()) {
                String key = normaliseEventName(handler.getEventName());
                handlersByEvent
                    .computeIfAbsent(key, k -> new ArrayList<>())
                    .add(handler);

                // Also allow firing all handlers via a synthetic "tests" event
                // so Fabric can execute them in test mode without knowing
                // their concrete event types yet.
                handlersByEvent
                    .computeIfAbsent("tests", k -> new ArrayList<>())
                    .add(handler);

                if (testNameForFile != null) {
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

        logger.info("Dispatching event '" + key + "' to " + handlers.size() + " handler(s)." +
            (context != null && context.getSubject() != null ? " subject=" + context.getSubject() : ""));

        for (ScriptEventHandler handler : handlers) {
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

