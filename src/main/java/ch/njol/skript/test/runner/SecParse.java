package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.core.parse.ParseLogsHolder;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import com.google.common.collect.Iterables;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Name("Parse Section")
@Description("Parse code inside this section and use 'parse logs' to grab any logs from it.")
@NoDoc
public class SecParse extends Section {

	static {
		Skript.registerSection(SecParse.class, "parse");
	}

	private String[] logs;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
		// Empty or comment-only section: treat as error but still load so script runs and test can assert on parse logs
		boolean hasCode = false;
		for (Node n : sectionNode) {
			String key = n.getKey();
			if (key != null && !key.trim().isEmpty() && !key.trim().startsWith("#")) {
				hasCode = true;
				break;
			}
		}
		if (!hasCode) {
			logs = new String[] { "A parse section must contain code" };
			return true;
		}

		RetainingLogHandler handler = SkriptLogger.startRetainingLog();
		// we need to do this before loadCode because loadCode will add this section to the current sections
		boolean inParseSection = getParser().isCurrentSection(SecParse.class);
		loadCode(sectionNode);
		if (!inParseSection) {
			// only store logs if we're not in another parse section.
			// this way you can access the parse logs of the outermost parse section
			logs = handler.getLog().stream()
					.map(LogEntry::getMessage)
					.toArray(String[]::new);
		}
		handler.stop();
		return true;
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		ExprParseLogs.lastLogs = logs;
		// Sync to core so "last parse logs" condition (CondParseLogs) sees the same value
		if (logs != null && logs.length > 0)
			ParseLogsHolder.set(String.join("\n", logs));
		else
			ParseLogsHolder.clear();
		return walk(event, false);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "parse";
	}

}
