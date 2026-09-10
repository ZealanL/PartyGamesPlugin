package zealan.pgp.game.games;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.Vec3i;

import java.util.ArrayList;

public class GameFireLeapers extends Game {
    public static final Vec3i SPAWN_CENTER = new Vec3i(-383, 89, 2156);
    public static final int AREA_SQUARE_RADIUS = 14;
    public static final BlockFace[] DIRS = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };

    private enum WaveKind {
        NORMAL,
        HEAL
    }

    private static class Wave {
        final WaveKind kind;
        final BlockFace dir;
        final double speed;
        double curOffset = -AREA_SQUARE_RADIUS;

        private Wave(WaveKind kind, BlockFace dir, double speed) {
            this.kind = kind;
            this.dir = dir;
            this.speed = speed;
        }
    }
    private final ArrayList<Wave> curWaves = new ArrayList<>();
    private int totalWaves = 0;
    private int spawnDelay = 0;

    public GameFireLeapers(InitParams params) {
        super(params);
    }

    @Override
    public void onLoaded() {
        super.onLoaded();

        Display.sendMsg(world, "&7[&e!&7] Make sure your particles are on...");
    }

    @Override
    public void spawnGamer(Gamer gamer) {
        super.spawnGamer(gamer);

        gamer.player.setMaxHealth(3*2);
        gamer.player.setHealth(3*2);

        gamer.player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.JUMP,
                        Integer.MAX_VALUE,
                        0,
                        true
                )
        );
    }

    @Override
    protected void innerOnStart() {

    }

    void spawnWave() {
        WaveKind kind = WaveKind.NORMAL;
        if (totalWaves > 10) {
            if (rand.nextInt(10) == 0) {
                // TODO: Fix heal visuals
                //kind = WaveKind.HEAL;
            }
        }

        double waveSpeed = 0.2 + Math.min(0.4, rand.nextDouble() * totalWaves * 0.02);
        BlockFace dir = DIRS[rand.nextInt(DIRS.length)];

        Wave wave = new Wave(kind, dir, waveSpeed);

        // Play spawning sound
        Vector centerPos = SPAWN_CENTER.getCenter();
        world.playSound(
                new Location(
                        world, centerPos.getX(), centerPos.getY(), centerPos.getZ()
                ),
                Sound.GHAST_FIREBALL,
                0.2f, 1.0f
        );

        curWaves.add(wave);
        totalWaves++;
        spawnDelay = 0;
    }

    @Override
    protected void innerOnTick() {
        if (!hasStarted())
            return;

        // Update spawning
        if (!hasEnded()) {
            if (spawnDelay <= 0) {
                spawnWave();

                int minSpawningDelay = 10 - (int)Math.clamp(totalWaves * 0.05, 0, 9);
                int maxSpawningDelay = minSpawningDelay * 2;
                spawnDelay = rand.nextInt(minSpawningDelay, maxSpawningDelay + 1);
            } else {
                spawnDelay--;
            }
        }

        // Update waves
        {
            Vector centerPos = SPAWN_CENTER.getCenter();
            for (Wave wave : curWaves) {
                Vector vecDir = new Vector(wave.dir.getModX(), 0, wave.dir.getModZ());
                Vector curCenterPos = SPAWN_CENTER.getCenter().add(vecDir.clone().multiply(wave.curOffset));

                Vector motion = vecDir.clone().multiply(wave.speed);

                Vector perpDir = new Vector(wave.dir.getModZ(), 0, wave.dir.getModX());
                for (double perpOffset = -AREA_SQUARE_RADIUS; perpOffset < AREA_SQUARE_RADIUS; perpOffset += 0.25) {
                    Vector curLinePos = curCenterPos.clone().add(
                            perpDir.clone().multiply(perpOffset)
                    );

                    world.spigot().playEffect(
                            new Location(world, curLinePos.getX(), curLinePos.getY(), curLinePos.getZ()),
                            switch (wave.kind) {
                                case NORMAL -> Effect.FLAME;
                                case HEAL -> Effect.HAPPY_VILLAGER;
                            },
                            0, 0,
                            (float) motion.getX(), 0, (float) motion.getZ(),
                            1.25f, 0, 50
                    );
                }

                double fromOffset = wave.curOffset;
                double toOffset = wave.curOffset + wave.speed;
                for (var gamer : getPlayingGamers()) {
                    final double JUMPING_Y = 89.35;
                    if (gamer.player.getLocation().getY() > JUMPING_Y)
                        continue;

                    Vector playerRelPos = gamer.player.getLocation().toVector().subtract(centerPos);
                    double playerOffset = playerRelPos.dot(vecDir);
                    final double FROM_PADDING = 0.25;
                    if (fromOffset - FROM_PADDING <= playerOffset && toOffset >= playerOffset) {
                        switch (wave.kind) {
                            case NORMAL -> gamer.player.damage(2);
                            case HEAL -> {
                                gamer.player.playSound(gamer.player.getLocation(), Sound.ORB_PICKUP, 0.75f, 1.0f);
                                gamer.player.setHealth(
                                        Math.min(gamer.player.getHealth() + 2, gamer.player.getMaxHealth())
                                );
                            }
                        }
                    }
                }

                wave.curOffset += wave.speed;
            }
            curWaves.removeIf(wave -> {
                return wave.curOffset > AREA_SQUARE_RADIUS;
            });
        }

        for (var gamer : getPlayingGamers()) {
            final double KILL_Y_TRESH = 87.5;
            if (gamer.player.getLocation().getY() < KILL_Y_TRESH)
                gamer.player.damage(999);
        }
    }

    @Override
    protected void innerOnEnd() {

    }
}
