package zealan.pgp.game.games;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.bossbar.BossBarContent;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BlockRange;
import zealan.pgp.math.Vec3i;

import java.util.ArrayList;
import java.util.HashSet;

public class GameAvalanche extends Game {
    public GameAvalanche(InitParams params) {
        super(params);
    }

    private static final int FLOOR_Y = 45;
    private static final int SNOWBALL_SPAWN_Y = 76;
    private static final BlockRange FLOOR_RANGE = BlockRange.fromCorners(
            -2405, FLOOR_Y, -1868,
            -2381, FLOOR_Y, -1892
    );
    private static final BlockRange COVER_BLOCKS_RANGE = FLOOR_RANGE.shift(new Vec3i(0, 4, 0));

    private static final BlockRange SNOWBALL_SPAWN_RANGE = BlockRange.fromCorners(
            -2407, SNOWBALL_SPAWN_Y, -1865,
            -2378, SNOWBALL_SPAWN_Y, -1894
    );

    // Confirmed from https://www.desmos.com/calculator/h3k7fl4trw
    private static final double SNOWBALL_VEL_MIN = -0.15;
    private static final double SNOWBALL_VEL_MAX = 0.15;

    private static final int SNOWBALLS_SPAWN_DURATION_TICKS = 15;
    private static final int SNOWBALLS_PER_TICK = 60;
    private static final int SNOWBALLS_PHASE_TICKS = 4 * 20;

    private record Wave(int prepareSecs, int numSafePoints) {}
    private static final Wave[] WAVES = {
            new Wave(8, 4),
            new Wave(7, 4),
            new Wave(6, 4),
            new Wave(5, 4),
            new Wave(4, 4),
            new Wave(3, 4),
            new Wave(3, 3),
            new Wave(3, 2),
            new Wave(3, 1),
    };

    private static final int FINAL_WAVE_IDX = 13;

    private int waveIdx = -1;
    private int waveTicksRemaining = -1;
    private final HashSet<Snowball> snowballs = new HashSet<>();

    private void progressWave() {
        waveIdx++;
        waveTicksRemaining = (getCurWave().prepareSecs() * 20) + SNOWBALLS_PHASE_TICKS;
        if (waveIdx > FINAL_WAVE_IDX) {
            end(true);
            return;
        }

        var possibles = new ArrayList<Vec3i>();
        for (Vec3i blockPos : COVER_BLOCKS_RANGE) {
            Block block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
            if (block.getType() != Material.AIR)
                block.setType(Material.AIR);
            possibles.add(blockPos);
        }

        for (int i = 0; i < getCurWave().numSafePoints(); i++) {
            int possibleIdx = rand.nextInt(possibles.size());
            Vec3i blockPos = possibles.get(possibleIdx);
            possibles.remove(possibleIdx);

            Block block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
            block.setType(Material.WOOD_STEP);
            world.playEffect(block.getLocation(), Effect.TILE_BREAK, Material.WOOD_STEP.getId());
            world.playSound(block.getLocation(), Sound.DIG_WOOD, 1.0f, 0.75f);
        }
    }

    private Wave getCurWave() {
        return WAVES[Math.max(0, Math.min(waveIdx, WAVES.length - 1))];
    }

    private boolean inSnowballPhase() {
        return waveTicksRemaining <= SNOWBALLS_PHASE_TICKS;
    }

    @Override
    protected void innerOnStart() {
        progressWave();
    }

    @Override
    protected void innerOnEnd() {
        for (Snowball snowball : snowballs) {
            if (snowball != null && snowball.isValid())
                snowball.remove();
        }
        snowballs.clear();
    }

    @Override
    public BossBarContent updateBossBar(Gamer gamer) {
        if (inSnowballPhase()) {
            return new BossBarContent("&c&l&oTAKE COVER!", 1.0f);
        } else {
            String name = "&e&lWave " + (waveIdx + 1);
            int ticksTillSnowballs = waveTicksRemaining - SNOWBALLS_PHASE_TICKS;
            double frac = (double) ticksTillSnowballs / (double) (getCurWave().prepareSecs() * 20);
            return new BossBarContent(name, (float) frac);
        }
    }

    @Override
    protected void innerOnTick() {
        if (!hasStarted())
            return;

        if (inSnowballPhase()) {
            int ticksInto = SNOWBALLS_PHASE_TICKS - waveTicksRemaining;
            if (ticksInto < SNOWBALLS_SPAWN_DURATION_TICKS) {
                for (int i = 0; i < SNOWBALLS_PER_TICK; i++) {
                    Vec3i spawnPos = SNOWBALL_SPAWN_RANGE.pickRandom(rand);

                    Location spawnLoc = new Location(world, spawnPos.x + 0.5, spawnPos.y, spawnPos.z + 0.5);
                    Snowball snowball = world.spawn(spawnLoc, Snowball.class);

                    double vx = SNOWBALL_VEL_MIN + rand.nextDouble() * (SNOWBALL_VEL_MAX - SNOWBALL_VEL_MIN);
                    double vz = SNOWBALL_VEL_MIN + rand.nextDouble() * (SNOWBALL_VEL_MAX - SNOWBALL_VEL_MIN);
                    snowball.setVelocity(new Vector(vx, 0, vz));

                    snowballs.add(snowball);
                }
            }
        }

        waveTicksRemaining--;
        if (waveTicksRemaining <= 0)
            progressWave();
    }

    @Override
    public void onProjectileHit(Projectile projectile) {
        if (!(projectile instanceof Snowball snowball) || !snowballs.remove(snowball))
            return;

        Location hitLoc = snowball.getLocation();
        Gamer nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Gamer gamer : getPlayingGamers()) {
            double distSq = gamer.player.getLocation().distanceSquared(hitLoc);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = gamer;
            }
        }

        if (nearest == null || nearestDistSq > 3.0)
            return;

        boolean blockAbovePlayer = false;
        Location playerLoc = nearest.player.getLocation();
        for (int i = 0; i < 5; i++) {
            Block above = world.getBlockAt(playerLoc.getBlockX(), playerLoc.getBlockY() + i, playerLoc.getBlockZ());
            if (above.getType() != Material.AIR) {
                blockAbovePlayer = true;
                break;
            }
        }

        if (!blockAbovePlayer) {
            nearest.player.damage(999);
            Display.sendMsg(world, "&7{} was hit by a snowball!", nearest.player);
        }
    }

    @Override
    public boolean canAttackEntity(Player player, Entity attacked, EntityDamageByEntityEvent event) {
        return waveIdx > WAVES.length;
    }
}