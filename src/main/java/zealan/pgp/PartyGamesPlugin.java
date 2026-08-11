package zealan.pgp;

import org.bukkit.plugin.java.JavaPlugin;
import zealan.pgp.api.command.CommandNode;
import zealan.pgp.api.command.CommandResult;

import static zealan.pgp.Globals.COMMAND_SYS;

public class PartyGamesPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        Globals.initialize(this);
    }
}