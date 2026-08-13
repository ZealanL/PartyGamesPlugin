package zealan.pgp;

import org.bukkit.plugin.java.JavaPlugin;

public class PartyGamesPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        Globals.initialize(this);
    }
}