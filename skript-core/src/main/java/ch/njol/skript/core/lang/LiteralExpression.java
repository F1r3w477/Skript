package ch.njol.skript.core.lang;

/**
 * Expression that always returns a fixed value. Use for parse-time literals
 * that do not depend on event or variables.
 *
 * @param <T> the type of the value
 */
public final class LiteralExpression<T> implements Expression<T> {

    private final T value;

    public LiteralExpression(T value) {
        this.value = value;
    }

    @Override
    public T get(ExecutionContext ctx) {
        return value;
    }
}
