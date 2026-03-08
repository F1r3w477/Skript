package ch.njol.skript.fabric.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites script content so that "on command \"name\":" is converted to "command /name:"
 * before the core parser sees it. Handles "on command \"a\" or \"b\":" by emitting two
 * command sections with the same body.
 */
public final class FabricScriptPreprocessor {

    // on command "name": or on command "name":
    private static final Pattern ON_COMMAND_SINGLE =
        Pattern.compile("^(\\s*)on\\s+command\\s+\"([^\"]+)\"\\s*:?\\s*$", Pattern.CASE_INSENSITIVE);
    // on command "a" or "b":
    private static final Pattern ON_COMMAND_OR =
        Pattern.compile("^(\\s*)on\\s+command\\s+\"([^\"]+)\"\\s+or\\s+\"([^\"]+)\"\\s*:?\\s*$", Pattern.CASE_INSENSITIVE);

    private FabricScriptPreprocessor() {}

    /**
     * Rewrites script content: "on command \"...\":" → "command /...:" and
     * "on command \"a\" or \"b\":" → two sections "command /a:" and "command /b:" with the same body.
     */
    public static String preprocess(String scriptContent) {
        if (scriptContent == null || scriptContent.isEmpty()) {
            return scriptContent;
        }
        String[] lines = scriptContent.split("\n", -1);
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            String lineTrimmed = line.trim();
            Matcher orMatcher = ON_COMMAND_OR.matcher(line);
            Matcher singleMatcher = ON_COMMAND_SINGLE.matcher(line);
            if (orMatcher.matches()) {
                String indent = orMatcher.group(1);
                String nameA = orMatcher.group(2).trim();
                String nameB = orMatcher.group(3).trim();
                List<String> body = collectBody(lines, i, indent);
                // Emit command /a: and command /b: each with same body
                out.add(indent + "command /" + nameA + ":");
                out.addAll(body);
                out.add("");
                out.add(indent + "command /" + nameB + ":");
                out.addAll(body);
                i += 1 + body.size();
                continue;
            }
            if (singleMatcher.matches()) {
                String indent = singleMatcher.group(1);
                String name = singleMatcher.group(2).trim();
                List<String> body = collectBody(lines, i, indent);
                out.add(indent + "command /" + name + ":");
                out.addAll(body);
                i += 1 + body.size();
                continue;
            }
            out.add(line);
            i++;
        }
        return String.join("\n", out);
    }

    /**
     * Collect lines that form the body of the section (strictly deeper indent than the header).
     */
    private static List<String> collectBody(String[] lines, int headerIndex, String headerIndent) {
        int headerDepth = headerIndent.length();
        List<String> body = new ArrayList<>();
        for (int j = headerIndex + 1; j < lines.length; j++) {
            String next = lines[j];
            int depth = leadingWhitespace(next);
            if (depth <= headerDepth && !next.trim().isEmpty()) {
                break;
            }
            body.add(next);
        }
        return body;
    }

    private static int leadingWhitespace(String s) {
        int i = 0;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }
}
