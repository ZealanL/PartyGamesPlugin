package zealan.pgp.api.command;

import zealan.pgp.api.display.Display;

public class CommandResult {
    public final boolean isSuccess;
    public final String msg;
    private CommandResult(boolean isSuccess, String msg) {
        this.isSuccess = isSuccess;
        this.msg = msg;
    }

    public static CommandResult ok() {
        return new CommandResult(true, null);
    }

    public static CommandResult ok(String goodText, Object... args) {
        return new CommandResult(true, Display.format("&a" + goodText, args));
    }
    public static CommandResult failure(String badText, Object... args) {
        return new CommandResult(false, Display.format("&c" + badText, args));
    }
}
