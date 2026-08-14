package zealan.pgp.game;

import org.bukkit.entity.Player;

import static zealan.pgp.Globals.*;

public class Gamer {
    public final int spawnIdx;
    public final Player player;
    protected Game game;

    public int points = 0;
    private int score = 0;
    private boolean isPlaying = true;
    private boolean hasFinished = false;
    private int ticksPlayedFor = 0;
    public int getPoints() { return points; }
    public int getScore() { return score; }
    public boolean isPlaying() { return isPlaying; }
    public boolean hasFinished() { return hasFinished; }
    public int getTicksPlayedFor() { return ticksPlayedFor; }

    public Gamer(int idx, Player player) {
        this.spawnIdx = idx;
        this.player = player;
    }

    public void setGame(Game game) {
        if (this.game != null)
            throw new IllegalStateException("Game already set");
        this.game = game;
    }
    public Game getGame() {
        return game;
    }

    public void stopPlaying(boolean finishedPlaying) {
        if (!isPlaying) {
            PLOG.severe(
                    "GamePlayer.stopPlaying() called when already done (game: \"" + game.config.properName + "\")"
            );
            return;
        }

        isPlaying = false;
        ticksPlayedFor = game.getTicksElapsed();
        hasFinished = finishedPlaying;

        if (finishedPlaying) {
            score = switch (game.config.style) {
                case POINTS -> points;
                case SURVIVAL -> game.getTicksElapsed();
                case RACE -> game.getTicksRemaining();
            };
            PLAYER_MGR.setFakeSpectator(player, true);
        } else {
            score = 0;
        }

        PLAYER_STATS_MGR.onPlayerStoppedPlaying(this, game, finishedPlaying);
    }
}
