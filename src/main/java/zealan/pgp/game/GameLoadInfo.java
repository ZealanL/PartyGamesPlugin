package zealan.pgp.game;

import zealan.pgp.math.BlockRange2d;

public class GameLoadInfo {
    public final BlockRange2d loadRange;
    public final GameSpawn[] spawns;

    public GameLoadInfo(BlockRange2d loadRange, GameSpawn... spawns) {
        this.loadRange = loadRange;
        this.spawns = spawns;
    }
}
