package zealan.pgp.game.games;

public class GameJungleJump extends GameParkourBase {
    public GameJungleJump(InitParams params) {
        super(params);
    }

    @Override
    protected int getCenterZ() {
        return 2310;
    }

    @Override
    protected int getCheckpointX() {
        return -223;
    }
}
