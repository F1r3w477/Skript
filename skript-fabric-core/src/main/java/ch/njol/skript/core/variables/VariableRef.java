package ch.njol.skript.core.variables;

/**
 * Reference to a variable by name (e.g. from parsing "{_x}").
 * Resolved at runtime via {@link EventValues#resolve(Object, ch.njol.skript.core.lang.ExecutionContext)}
 * using the current {@link VariableScope}.
 */
public final class VariableRef {

    private final String name;

    public VariableRef(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getName() {
        return name;
    }
}
