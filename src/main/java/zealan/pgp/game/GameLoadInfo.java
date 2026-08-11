package zealan.pgp.game;

import org.bukkit.Location;

public class GameLoadInfo {
    public final Location[] spawns;

    public GameLoadInfo(Location... spawns) {
        this.spawns = spawns;
    }
}
