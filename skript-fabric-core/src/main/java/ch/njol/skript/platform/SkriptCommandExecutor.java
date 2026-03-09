package ch.njol.skript.platform;

/**
 * Callback invoked when a registered Skript command is executed.
 * The platform resolves the sender and arguments and calls this interface.
 */
@FunctionalInterface
public interface SkriptCommandExecutor {

    /**
     * Handle the command. The platform guarantees this runs on the main thread.
     *
     * @param sender who executed the command (player or console)
     * @param args   arguments after the command name (never null)
     */
    void execute(SkriptCommandSender sender, String[] args);
}
