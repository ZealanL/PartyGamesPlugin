package zealan.pgp.game.games;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;

import java.util.HashSet;

public abstract class GameParkourBase extends Game {

    private static final int GATE_X = -183;
    private static final int GATE_Y = 7;
    private static final int CHECKPOINT_Y = 7;
    private static final int FINISH_X = -298;
    private static final int FINISH_MIN_Y = 10;
    private static final int FAIL_FALL_Y = 3;

    protected GameParkourBase(InitParams params) {
        super(params);
    }

    protected abstract int getCenterZ();
    protected abstract int getCheckpointX();

    private final HashSet<Gamer> checkpointedPlayers = new HashSet<>();

    @Override
    protected void innerOnStart() {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int gateX = GATE_X + dx;
                int gateY = GATE_Y;
                int gateZ = getCenterZ() + dz;
                Block block = world.getBlockAt(gateX, gateY, gateZ);
                if (!block.isEmpty()) {
                    block.setType(Material.AIR);
                }
            }
        }

        for (Gamer gamer : getPlayingGamers()) {
            if (gamer.player.getLocation().getX() <= GATE_X - 0.2)
                spawnGamer(gamer);
            gamer.player.setWalkSpeed(0.35f);
        }
    }

    @Override
    public void spawnGamer(Gamer gamer) {
        super.spawnGamer(gamer);
        if (hasStarted())
            gamer.player.setWalkSpeed(0.35f);
    }

    @Override
    protected void innerOnTick() {
        for (Gamer gamer : getGamers()) {
            Location loc = gamer.player.getLocation();
            int blockX = loc.getBlockX();
            int blockY = loc.getBlockY();

            if (blockY <= FAIL_FALL_Y) {
                if (checkpointedPlayers.contains(gamer)) {
                    EntityUtil.setPosOnly(
                            gamer.player,
                            new Vector(getCheckpointX() + 0.5, CHECKPOINT_Y, getCenterZ() + 0.5)
                    );
                } else {
                    spawnGamer(gamer);
                }
            }

            if (!checkpointedPlayers.contains(gamer)) {
                if (blockX == getCheckpointX() && gamer.player.isOnGround()) {
                    Display.sendMsg(gamer.player, "&aCheckpoint reached!");
                    gamer.player.playSound(loc, Sound.NOTE_PLING, 0.8f, 1.75f);
                    checkpointedPlayers.add(gamer);
                }
            } else if (blockX == FINISH_X && blockY >= FINISH_MIN_Y && gamer.player.isOnGround()) {
                gamer.player.playSound(loc, Sound.LEVEL_UP, 0.8f, 1.0f);
                double finishTime = getTicksElapsed() / 20.0;
                Display.sendMsg(gamer.player, "&aYou finished the parkour in " + finishTime + "s!");
                gamer.stopPlaying(true);
            }
        }
    }

    @Override
    protected void innerOnEnd() {

    }
}