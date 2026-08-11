package zealan.pgp.api.command;

@FunctionalInterface
public interface CommandFunc {
    CommandResult execute(CommandCtx ctx) throws CommandSyntaxException;
}
