package zealan.pgp.game;

import zealan.pgp.math.Vec3i;

public class GameLoadInfo {
    public final Vec3i regionAroundLoc;
    public final GameSpawn[] spawns;

    public GameLoadInfo(Vec3i regionAroundLoc, GameSpawn... spawns) {
        this.regionAroundLoc = regionAroundLoc;
        this.spawns = spawns;
    }

    public GameLoadInfo(GameSpawn... spawns) {
        this.regionAroundLoc = Vec3i.from(spawns[0].pos);
        this.spawns = spawns;
    }
}
