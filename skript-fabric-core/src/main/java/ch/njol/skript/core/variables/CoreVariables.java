package ch.njol.skript.core.variables;

/**
 * Thread-local current variable scope for the running trigger. The runtime
 * sets the scope at the start of each handler and clears it after. Scripts
 * can use "set {_var} to x" and "{_var}" via this API. Persistence (e.g. file)
 * is not in core; platforms may add a layer on top.
 */
public final class CoreVariables {

    private static final ThreadLocal<VariableScope> CURRENT_SCOPE = new ThreadLocal<>();

    /**
     * Set the scope for the current execution. Called by the runtime at the start of a handler.
     */
    public static void setScope(VariableScope scope) {
        CURRENT_SCOPE.set(scope);
    }

    /**
     * Get the current scope, or null if not in a trigger execution.
     */
    public static VariableScope getScope() {
        return CURRENT_SCOPE.get();
    }

    /**
     * Clear the current scope. Called by the runtime after a handler finishes.
     */
    public static void clearScope() {
        CURRENT_SCOPE.remove();
    }

    /**
     * Get a variable from the current scope. Returns null if no scope or variable not set.
     */
    public static Object get(String name) {
        VariableScope scope = getScope();
        return scope != null ? scope.get(name) : null;
    }

    /**
     * Set a variable in the current scope. No-op if no scope.
     */
    public static void set(String name, Object value) {
        VariableScope scope = getScope();
        if (scope != null) {
            scope.set(name, value);
        }
    }

    /**
     * Remove a variable from the current scope. No-op if no scope.
     */
    public static void remove(String name) {
        VariableScope scope = getScope();
        if (scope != null) {
            scope.remove(name);
        }
    }
}
