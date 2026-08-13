package zealan.pgp.game;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import zealan.pgp.api.display.Display;
import zealan.pgp.bossbar.BossBarContent;
import zealan.pgp.util.PlayerUtil;
import zealan.pgp.worldevents.WorldEvents;

import java.util.List;
import java.util.Random;

import static zealan.pgp.Globals.BOSS_BAR_MGR;
import static zealan.pgp.Globals.PLAYER_MGR;

public abstract class Game implements WorldEvents {
    public final GameConfig config;
    public final GameVariant variant;
    public final List<Gamer> gamers;
    public final World world;

    public final Random rand = new Random();

    public enum State {
        STARTING,
        RUNNING,
        ENDED
    }
    private State state = State.STARTING;

    public record InitParams(GameConfig config, GameVariant variant, List<Gamer> gamers, World world) { }

    public Game(InitParams params) {
        this.config = params.config;
        this.variant = params.variant;
        this.gamers = params.gamers;
        this.world = params.world;
    }

    // ///////////////

    private int countdownTicks = 4 * 20;
    private int ticksElapsed = 0;

    public final int getTicksElapsed() {
        return ticksElapsed;
    }

    public final int getTicksRemaining() {
        return Math.max(config.maxDurationTicks() - ticksElapsed, 0);
    }

    public final List<Gamer> getGamers() {
        return gamers;
    }

    public final List<Gamer> getPlayingGamers() {
        return gamers.stream().filter(Gamer::isPlaying).toList();
    }

    public final Gamer getGamerFrom(Player player) {
        return gamers.stream().filter(g -> g.player == player).findFirst().orElse(null);
    }

    public final State getState() { return state; }
    public final boolean hasStarted() { return state != State.STARTING; }
    public final boolean hasEnded() { return state == State.ENDED; }

    public void onLoaded() {
        for (var gamer : gamers)
            spawnGamer(gamer);

        world.setTime(3000);
        start();
    }

    private void start() {
        innerOnStart();
    }
    public final void end() {
        end(false);
    }
    protected final void end(boolean endedNaturally) {
        state = State.ENDED;
        for (var gamer : getPlayingGamers())
            gamer.stopPlaying(endedNaturally);
        innerOnEnd();
    }

    public final void onTick() {
        innerOnTick(state != State.STARTING);

        switch (state) {
            case STARTING -> {
                if (countdownTicks > 0) {
                    if (countdownTicks % 20 == 0) {
                        for (var gamer : this.gamers) {
                            Display.sendTitle(gamer.player, "&a" + (countdownTicks / 20), "");
                            gamer.player.playSound(
                                    gamer.player.getLocation(), Sound.CLICK, 0.9f, 1.0f
                            );
                        }
                    }
                    countdownTicks--;
                } else {
                    for (var gamer : this.gamers) {
                        Display.sendTitle(gamer.player, "", "");
                        gamer.player.playSound(
                                gamer.player.getLocation(), Sound.CLICK, 0.9f, 2.0f
                        );
                    }
                    state = State.RUNNING;
                    start();
                }
            }
            case RUNNING -> {
                ticksElapsed++;

                for (var gamer : this.gamers)
                    BOSS_BAR_MGR.setForPlayer(gamer.player, updateBossBar(gamer));

                if (ticksElapsed > config.maxDurationTicks()) {
                    end(true);
                } else if (getPlayingGamers().isEmpty()) {
                    end(true);
                }
            }
        }
    }

    public final GameSpawn getGamerSpawn(Gamer gamer) {
        var info = config.loadInfo;
        var spawns = info.spawns;
        if (spawns.length < 1)
            throw new RuntimeException("LoadInfo doesn't have any spawnpoints!");

        return spawns[gamer.spawnIdx % info.spawns.length];
    }

    public void spawnGamer(Gamer gamer) {
        PlayerUtil.cleanPlayer(gamer.player);

        PLAYER_MGR.setFakeSpectator(gamer.player, false);
        var spawn = getGamerSpawn(gamer);
        gamer.player.teleport(spawn.toLocation(world));
        gamer.player.setGameMode(config.gameMode);
    }

    public BossBarContent updateBossBar(Gamer gamer) {
        int secondsElapsed = getTicksElapsed() / 20;
        int secondsRemaining = getTicksRemaining() / 20;
        return switch (config.style) {
            case RACE, POINTS -> new BossBarContent(
                    Display.format("Time left: {}s", secondsRemaining),
                    getTicksRemaining() / (double)config.maxDurationTicks()
            );
            case SURVIVAL -> new BossBarContent(
                    Display.format("Survived for: {}s", secondsElapsed),
                    1.0
            );
        };
    }

    // ////////

    protected abstract void innerOnStart();
    protected abstract void innerOnTick(boolean hasStarted);
    protected abstract void innerOnEnd();

    @Override
    public void onPlayerDeath(Player player) {
        var gamer = getGamerFrom(player);
        if (gamer != null) {
            spawnGamer(gamer);
            gamer.stopPlaying(config.style != GameStyle.RACE);
        }
    }
}
