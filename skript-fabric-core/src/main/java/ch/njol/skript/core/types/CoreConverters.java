package ch.njol.skript.core.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal converters: given type A and B, try to convert A → B.
 * Used by the pattern parser when a placeholder type doesn't match exactly
 * (e.g. "%object%" and we have a player). No chaining for now; platforms
 * register direct conversions.
 */
public final class CoreConverters {

    private static final CoreConverters INSTANCE = new CoreConverters();

    public static CoreConverters get() {
        return INSTANCE;
    }

    @FunctionalInterface
    public interface Converter<F, T> {
        T convert(F from);
    }

    private static final class Entry<F, T> {
        final Class<F> from;
        final Class<T> to;
        final Converter<F, T> converter;

        Entry(Class<F> from, Class<T> to, Converter<F, T> converter) {
            this.from = from;
            this.to = to;
            this.converter = converter;
        }
    }

    private final List<Entry<?, ?>> converters = new ArrayList<>();

    private CoreConverters() {
        registerBuiltins();
    }

    @SuppressWarnings("unchecked")
    private void registerBuiltins() {
        register(String.class, Number.class, s -> {
            if (s == null || s.isEmpty()) return null;
            try {
                if (s.contains(".")) return Double.parseDouble(s);
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        });
        register(Number.class, String.class, n -> n == null ? null : n.toString());
        register(Number.class, Long.class, n -> n == null ? null : n.longValue());
        register(Number.class, Double.class, n -> n == null ? null : n.doubleValue());
        register(Boolean.class, String.class, b -> b == null ? null : b.toString());
        register(String.class, Boolean.class, s -> {
            if (s == null) return null;
            String t = s.trim().toLowerCase();
            if ("true".equals(t)) return true;
            if ("false".equals(t)) return false;
            return null;
        });
    }

    public <F, T> void register(Class<F> fromType, Class<T> toType, Converter<F, T> converter) {
        converters.add(new Entry<>(fromType, toType, converter));
    }

    /**
     * Convert the given object to the target type. Returns null if no converter exists or conversion fails.
     */
    @SuppressWarnings("unchecked")
    public <T> T convert(Object from, Class<T> toType) {
        if (from == null) return null;
        if (toType.isInstance(from)) return (T) from;
        for (Entry<?, ?> e : converters) {
            if (e.from.isInstance(from) && e.to == toType) {
                Object out = ((Entry<Object, Object>) e).converter.convert(from);
                return toType.isInstance(out) ? (T) out : null;
            }
        }
        return null;
    }

    public boolean converterExists(Class<?> fromType, Class<?> toType) {
        if (fromType.isAssignableFrom(toType)) return true;
        for (Entry<?, ?> e : converters) {
            if (e.from.isAssignableFrom(fromType) && e.to == toType) return true;
        }
        return false;
    }
}
