package ch.njol.skript.core.patterns;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for CorePatternCompiler and CoreSkriptPattern (optional, choice, group).
 */
class CorePatternCompilerTest {

    @Test
    void literalOnly() {
        CoreSkriptPattern p = CorePatternCompiler.compile("hello");
        CoreSkriptPattern.CoreMatchResult r = p.match("hello");
        assertNotNull(r);
        assertEquals(0, r.expressions.length);
        assertNull(p.match("hell"));
        assertNull(p.match("hello world"));
    }

    @Test
    void typeSlots() {
        CoreSkriptPattern p = CorePatternCompiler.compile("say %string%");
        CoreSkriptPattern.CoreMatchResult r = p.match("say \"hi\"");
        assertNotNull(r);
        assertEquals(1, r.expressions.length);
        assertEquals("hi", r.getExpression(0));
    }

    @Test
    void optionalMatchesWithOrWithout() {
        // No space between ] and % so chain is: set -> optional -> variable -> " to " -> object
        CoreSkriptPattern p = CorePatternCompiler.compile("set [the]%variable% to %object%");
        CoreSkriptPattern.CoreMatchResult r1 = p.match("set {_x} to 5");
        assertNotNull(r1);
        assertEquals(2, r1.expressions.length);
        assertNull(p.match("set to 5"));
    }

    @Test
    void optionalAtEnd() {
        // No space between %string% and [ so "broadcast \"hello\"" matches (optional skipped)
        CoreSkriptPattern p = CorePatternCompiler.compile("broadcast %string%[ to %players%]");
        CoreSkriptPattern.CoreMatchResult r = p.match("broadcast \"hello\"");
        assertNotNull(r);
        assertEquals(2, r.expressions.length);
        assertEquals("hello", r.getExpression(0));
    }

    @Test
    void choiceMatchesOneBranch() {
        CoreSkriptPattern p = CorePatternCompiler.compile("one|two|three");
        assertNotNull(p.match("one"));
        assertNotNull(p.match("two"));
        assertNotNull(p.match("three"));
        assertNull(p.match("four"));
        assertNull(p.match("on"));
    }

    @Test
    void groupMatchesInner() {
        CoreSkriptPattern p = CorePatternCompiler.compile("(foo) bar");
        CoreSkriptPattern.CoreMatchResult r = p.match("foo bar");
        assertNotNull(r);
    }

    @Test
    void nextBracketNesting() {
        // start is index *after* the opening bracket (so 2 for "a[b]c" where '[' is at 1)
        int end = CorePatternCompiler.nextBracket("a[b]c", ']', '[', 2, true);
        assertEquals(3, end);
        end = CorePatternCompiler.nextBracket("a[b[c]d]e", ']', '[', 2, true);
        assertEquals(7, end);
    }

    @Test
    void unclosedBracketThrows() {
        assertThrows(MalformedPatternException.class, () ->
            CorePatternCompiler.compile("set [the %variable% to 5"));
    }

    @Test
    void unclosedPercentThrows() {
        assertThrows(MalformedPatternException.class, () ->
            CorePatternCompiler.compile("set %variable to 5"));
    }

    @Test
    void backslashEscape() {
        CoreSkriptPattern p = CorePatternCompiler.compile("\\[literal\\]");
        CoreSkriptPattern.CoreMatchResult r = p.match("[literal]");
        assertNotNull(r);
    }

}
