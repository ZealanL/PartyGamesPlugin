package zealan.pgp.api.command;

import org.bukkit.entity.Player;

import java.util.HashMap;

public class CommandCtx {
    public final Player sender;

    private final HashMap<String, Object> passedArgs = new HashMap<>();

    public CommandCtx(Player sender) {
        this.sender = sender;
    }

    public void setPassedArg(String name, Object value) {
        if (passedArgs.containsKey(name))
            throw new IllegalArgumentException("Duplicate argument \"" + name + "\"");
        passedArgs.put(name, value);
    }

    public <T> T getArg(String name) throws CommandSyntaxException {
        if (passedArgs.containsKey(name)) {
            try {
                return (T)passedArgs.get(name);
            } catch (RuntimeException ignored) {
                return null;
            }
        } else {
            return null;
        }
    }
}
