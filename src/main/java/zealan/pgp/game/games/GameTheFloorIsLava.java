package zealan.pgp.game.games;

public class GameTheFloorIsLava extends GameParkourBase {
    public GameTheFloorIsLava(InitParams params) {
        super(params);
    }

    @Override
    protected int getCenterZ() {
        return 2293;
    }

    @Override
    protected int getCheckpointX() {
        return -222;
    }
}
