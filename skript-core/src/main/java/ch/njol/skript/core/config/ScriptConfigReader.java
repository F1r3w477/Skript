package ch.njol.skript.core.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Line-by-line reader for script config files. Tracks line number and supports
 * reset() to re-read the last line (used when a line has less indent and belongs
 * to the parent section). Strips UTF-8 BOM on first non-empty line.
 */
final class ScriptConfigReader {

    private final BufferedReader reader;
    private int lineNum = 0;
    private String lastLine;
    private boolean resetRequested = false;
    private boolean hadNonEmptyLine = false;

    ScriptConfigReader(InputStream in) {
        this.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    String readLine() throws IOException {
        if (resetRequested) {
            resetRequested = false;
            return lastLine;
        }
        lastLine = reader.readLine();
        if (lastLine != null) {
            lineNum++;
            if (!hadNonEmptyLine && !lastLine.isEmpty()) {
                hadNonEmptyLine = true;
                if (lastLine.startsWith("\uFEFF")) {
                    lastLine = lastLine.substring(1);
                }
            }
        }
        return lastLine;
    }

    void reset() {
        if (resetRequested) {
            throw new IllegalStateException("reset() called twice without readLine() in between");
        }
        resetRequested = true;
    }

    int getLineNum() {
        return lineNum;
    }
}
