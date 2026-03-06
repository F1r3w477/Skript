package ch.njol.skript.core.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Phase 4 type system (CoreTypes, CoreConverters, ParseContext).
 */
class CoreTypesTest {

    @Test
    void parseContextHolderDefaultsToDefault() {
        ParseContextHolder.clear();
        assertEquals(ParseContext.DEFAULT, ParseContextHolder.get());
    }

    @Test
    void parseContextHolderSetAndGet() {
        ParseContextHolder.set(ParseContext.CONFIG);
        assertEquals(ParseContext.CONFIG, ParseContextHolder.get());
        ParseContextHolder.set(ParseContext.DEFAULT);
        assertEquals(ParseContext.DEFAULT, ParseContextHolder.get());
        ParseContextHolder.clear();
    }

    @Test
    void coreTypesParseStringAndNumber() {
        CoreTypes types = CoreTypes.get();
        assertEquals("hello", types.parse("string", "hello", ParseContext.DEFAULT));
        Object num = types.parse("number", "42", ParseContext.DEFAULT);
        assertNotNull(num);
        assertEquals(42L, num);
        Object d = types.parse("number", "3.14", ParseContext.DEFAULT);
        assertNotNull(d);
        assertEquals(3.14, d);
    }

    @Test
    void coreTypesParseBoolean() {
        CoreTypes types = CoreTypes.get();
        assertTrue((Boolean) types.parse("boolean", "true", ParseContext.DEFAULT));
        assertEquals(Boolean.FALSE, types.parse("boolean", "false", ParseContext.DEFAULT));
        assertNull(types.parse("boolean", "x", ParseContext.DEFAULT));
    }

    @Test
    void coreConvertersConvert() {
        CoreConverters conv = CoreConverters.get();
        assertEquals(42L, conv.convert("42", Number.class));
        assertEquals("42", conv.convert(42L, String.class));
        assertTrue(conv.converterExists(String.class, Number.class));
    }
}
