package zealan.pgp.game;

public class Game {
    private int ticksElapsed = 0;
    public final GameConfig config;
    public final GameVariant variant;

    public Game(GameConfig config, GameVariant variant) {
        this.config = config;
        this.variant = variant;
    }

    public final int getTicksElapsed() {
        return ticksElapsed;
    }

    public final int getTicksRemaining() {
        return Math.max(config.maxDurationTicks() - ticksElapsed, 0);
    }
}
