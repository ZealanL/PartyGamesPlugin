package zealan.pgp.game;

import org.bukkit.Location;
import zealan.pgp.math.Vec3i;

public class GameLoadInfo {
    public final GameSpawn[] spawns;

    public GameLoadInfo(GameSpawn... spawns) {
        this.spawns = spawns;
    }
}
