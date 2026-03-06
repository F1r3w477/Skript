package ch.njol.skript.core.variables;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution-scoped variable storage for one trigger run. get/set/remove by name.
 * Core keeps the API in-memory; persistence can be platform-specific. Used so
 * "set {_x} to 1" and "{_x}" work within a script execution.
 */
public final class VariableScope {

    private final Map<String, Object> storage = new HashMap<>();

    public Object get(String name) {
        return storage.get(normalise(name));
    }

    public void set(String name, Object value) {
        storage.put(normalise(name), value);
    }

    public void remove(String name) {
        storage.remove(normalise(name));
    }

    public boolean has(String name) {
        return storage.containsKey(normalise(name));
    }

    private static String normalise(String name) {
        if (name == null) return "";
        String t = name.trim();
        if (t.startsWith("{") && t.endsWith("}")) {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }
}
