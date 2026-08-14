package zealan.pgp.game.games;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFallingSand;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.bossbar.BossBarContent;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BoundingBox;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;

import java.util.ArrayList;

public class GameBombardment extends Game {
    public static final GameVariant VARIANT_IMPATIENT = new GameVariant(
            "impatient", "Impatient", "Sudden death starts immediately!",
            Material.WATCH
    );
    public static final GameVariant VARIANT_FAST_CANNONS = new GameVariant(
            "fast_cannons", "Fast Cannons", "Cannonballs move twice as fast!",
            Material.DISPENSER
    );

    private static final int FLOOR_Y = 19;
    private static final int WATER_KILL_Y_THRESH = 16;
    private static final Vec3i[] CANNON_SPAWNS = {
            new Vec3i(-352, 51, 735),
            new Vec3i(-352, 51, 729),
            new Vec3i(-352, 51, 723),
            new Vec3i(-352, 51, 717),
    };

    private static final double CANNONBALL_SPEED = 20.0;
    private static final double EXPLOSION_KILL_RADIUS = 2.0;

    private static final int SUDDEN_DEATH_BURST_TICKS = 26;
    private static final int SUDDEN_DEATH_CYCLE_TICKS = 10 * 20;
    private static final int TIME_UNTIL_SUDDEN_DEATH = 60;

    private int suddenDeathTimer = TIME_UNTIL_SUDDEN_DEATH;
    private int suddenDeathCycleTick = 0;
    private int firingInterval = 8;
    private int firingCount = 1;
    private int firingTimer = 0;

    private static class CannonBall {
        final FallingBlock fbe;
        final LivingEntity vehicle;
        final Vector startPos;
        final Vector vel;

        CannonBall(FallingBlock fbe, LivingEntity vehicle, Vector startPos, Vector vel) {
            this.fbe = fbe;
            this.vehicle = vehicle;
            this.startPos = startPos;
            this.vel = vel;
        }
    }

    private final ArrayList<CannonBall> cannonBalls = new ArrayList<>();

    public GameBombardment(InitParams params) {
        super(params);
        if (variant == VARIANT_IMPATIENT) {
            suddenDeathTimer = 0;
        }
    }

    @Override
    protected void innerOnStart() {
    }

    @Override
    protected void innerOnEnd() {
        for (var c : cannonBalls) {
            if (c.fbe != null && c.fbe.isValid()) {
                c.fbe.remove();
            }
            if (c.vehicle != null && c.vehicle.isValid()) {
                c.vehicle.remove();
            }
        }
        cannonBalls.clear();
    }

    @Override
    public BossBarContent updateBossBar(Gamer gamer) {
        if (suddenDeathTimer > 0) {
            String name = "&l&cSudden Death In: &e" + suddenDeathTimer + "s";
            float progress = suddenDeathTimer / (float) TIME_UNTIL_SUDDEN_DEATH;
            return new BossBarContent(name, progress);
        } else {
            boolean isIn = isInSuddenDeathBurst();
            String name = "&l" + (isIn ? "&dSUDDEN DEATH!" : "&c...");

            float progress;
            if (isIn) {
                float burstFrac = (suddenDeathCycleTick + 1) / (float) SUDDEN_DEATH_BURST_TICKS;
                progress = 1.0f - burstFrac;
            } else {
                progress = (suddenDeathCycleTick + 1) / (float) SUDDEN_DEATH_CYCLE_TICKS;
            }

            return new BossBarContent(name, progress);
        }
    }

    private boolean isInSuddenDeathBurst() {
        return (suddenDeathTimer == 0) && (suddenDeathCycleTick < SUDDEN_DEATH_BURST_TICKS);
    }

    private int calcTargetingMax() {
        if (suddenDeathTimer <= 17) {
            var num = 1;
            if (suddenDeathTimer <= 15) num++;
            if (suddenDeathTimer <= 8) num++;
            return num;
        } else {
            return 0;
        }
    }

    private void updateFiringSpeeds() {
        int interval = 8;
        int count = 1;
        if (suddenDeathTimer <= 46) interval /= 2;
        if (suddenDeathTimer <= 42) interval /= 2;
        if (suddenDeathTimer <= 30) interval /= 2;
        if (suddenDeathTimer <= 27) count += 2;
        if (suddenDeathTimer <= 18) count += 1;

        String updateMsgPrefix = "&6[&l!&r&6] ";
        if (interval != this.firingInterval) {
            Display.sendMsg(world, updateMsgPrefix + "&o&6The ship now fires every " + interval + "s...");
        }
        if (count != this.firingCount) {
            Display.sendMsg(world, updateMsgPrefix + "&o&6The ship now fires " + count + " shots at once!");
        }

        this.firingInterval = interval;
        this.firingCount = count;
    }

    private Vector genCannonTarget(boolean targetRandomPlayer) {
        if (targetRandomPlayer) {
            var playingGamers = getPlayingGamers();
            if (playingGamers.isEmpty()) {
                return new Vector(0, 0, 0);
            }
            var targetGamer = playingGamers.get(rand.nextInt(playingGamers.size()));
            Location loc = targetGamer.player.getLocation();
            return new Vector(loc.getBlockX() + 0.5, FLOOR_Y, loc.getBlockZ() + 0.5);
        } else {
            final Vec3i FLOOR_MIN = new Vec3i(-400, FLOOR_Y, 711);
            final Vec3i FLOOR_MAX = new Vec3i(-384, FLOOR_Y, 741);

            for (int i = 0; i < 100; i++) {
                int rx = rand.nextInt(FLOOR_MAX.x - FLOOR_MIN.x + 1) + FLOOR_MIN.x;
                int ry = rand.nextInt(FLOOR_MAX.y - FLOOR_MIN.y + 1) + FLOOR_MIN.y;
                int rz = rand.nextInt(FLOOR_MAX.z - FLOOR_MIN.z + 1) + FLOOR_MIN.z;

                Block block = world.getBlockAt(rx, ry, rz);
                if (block.getType() != Material.AIR) {
                    return new Vector(rx + 0.5, ry, rz + 0.5);
                }
            }
            return new Vector(0, 0, 0);
        }
    }

    private void spawnCannonBalls(int count) {
        int maxTargeting = calcTargetingMax();
        int numTargeting = Math.min(
                Math.max(rand.nextInt(maxTargeting + 1), rand.nextInt(maxTargeting + 1)),
                count - 1
        );

        for (int i = 0; i < count; i++) {
            var cannonIdx = rand.nextInt(CANNON_SPAWNS.length);
            var spawnPos = CANNON_SPAWNS[cannonIdx];
            Vector spawnPosCenter = new Vector(spawnPos.x + 0.5, spawnPos.y + 0.5, spawnPos.z + 0.5);

            Location loc = new Location(world, spawnPosCenter.getX(), spawnPosCenter.getY(), spawnPosCenter.getZ());

            FallingBlock fbe = world.spawnFallingBlock(loc, Material.COAL_BLOCK, (byte) 0);
            fbe.setDropItem(false);

            ArmorStand vehicle = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
            vehicle.setVisible(true);
            vehicle.setGravity(false);
            vehicle.setSmall(true);
            vehicle.setMarker(true);
            vehicle.setPassenger(fbe);

            Vector targetPos = genCannonTarget(i < numTargeting);
            Vector targetDir = targetPos.clone().subtract(spawnPosCenter).normalize();
            Vector vel = targetDir.multiply(CANNONBALL_SPEED / 20.0);
            if (variant == VARIANT_FAST_CANNONS) {
                vel.multiply(2.0);
            }

            //vehicle.setVelocity(vel);

            world.createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0.0f, false, false);
            world.spigot().playEffect(
                    loc, Effect.EXPLOSION_LARGE,
                    0, 1,
                    0.3f, 0.3f, 0.3f,
                    0.2f, 1, 64
            );
            cannonBalls.add(new CannonBall(fbe, vehicle, spawnPosCenter, vel));
        }
    }

    @Override
    protected void innerOnTick() {
        if (!hasStarted())
            return;

        for (var gamer : getPlayingGamers()) {
            if (gamer.player.getLocation().getBlockY() <= WATER_KILL_Y_THRESH) {
                gamer.player.damage(999);
                Display.sendMsg(world, "&7{} fell into the water!", gamer.player);
            }
        }

        // Update cannonballs
        for (CannonBall cannonBall : cannonBalls) {
            LivingEntity vehicle = cannonBall.vehicle;
            if (vehicle != null && vehicle.isValid()) {
                long ticksElapsed = cannonBall.fbe.getTicksLived();
                Vector curPos = cannonBall.startPos.clone().add(cannonBall.vel.clone().multiply(ticksElapsed));

                Vector showPos = curPos.clone().add(cannonBall.vel.clone().multiply(2));
                EntityUtil.spoofEntityMove(vehicle, showPos, false);

                if (getTicksElapsed() % 2 == 0) {
                    Vector particlePos = curPos.clone().subtract(cannonBall.vel.clone().multiply(3));
                    world.spigot().playEffect(
                            new Location(world, particlePos.getX(), particlePos.getY(), particlePos.getZ()),
                            Effect.LAVA_POP, 0, 0,
                            0f, 0.5f, 0f, 0.02f, 4, 64
                    );
                }

                if (curPos.getY() <= FLOOR_Y + 1) {
                    Location explodeLoc = new Location(world, curPos.getX(), FLOOR_Y + 1, curPos.getZ());
                    world.playSound(explodeLoc, Sound.EXPLODE, 1.0f, 1.0f);
                    world.spigot().playEffect(
                            explodeLoc, Effect.EXPLOSION,
                            0, 1,
                            0.3f, 0.3f, 0.3f,
                            0.2f, 5, 64
                    );
                    world.spigot().playEffect(
                            explodeLoc, Effect.EXPLOSION_LARGE,
                            0, 1,
                            1.0f, 1.0f, 1.0f,
                            0.2f, 1, 64
                    );

                    vehicle.eject();
                    vehicle.teleport(explodeLoc);
                    vehicle.setPassenger(cannonBall.fbe);

                    cannonBall.fbe.remove();
                    vehicle.remove();

                    var cannonBallHitbox = new BoundingBox(
                            curPos.getX() - EXPLOSION_KILL_RADIUS, FLOOR_Y + 1 - EXPLOSION_KILL_RADIUS, curPos.getZ() - EXPLOSION_KILL_RADIUS,
                            curPos.getX() + EXPLOSION_KILL_RADIUS, FLOOR_Y + 1 + EXPLOSION_KILL_RADIUS, curPos.getZ() + EXPLOSION_KILL_RADIUS
                    );

                    for (var gamer : getPlayingGamers()) {
                        var playerBox = EntityUtil.getEntityHitbox(gamer.player);
                        if (cannonBallHitbox.intersects(playerBox)) {
                            Display.sendMsg(world, "&7{} was blown to pieces!", gamer.player);
                            gamer.player.damage(999);
                        }
                    }
                }
            } else {
                if (cannonBall.fbe != null) cannonBall.fbe.remove();
            }
        }
        cannonBalls.removeIf(cb -> cb.fbe == null || !cb.fbe.isValid());

        if (isInSuddenDeathBurst()) {
            spawnCannonBalls(1);
            firingTimer = 1;
        } else {
            if (this.getTicksElapsed() % 20 == 0) {
                updateFiringSpeeds();
                if (firingTimer == 0) {
                    spawnCannonBalls(firingCount);
                    firingTimer = firingInterval;
                }
                firingTimer--;
            }
        }

        if (suddenDeathTimer > 0) {
            if (this.getTicksElapsed() % 20 == 0) {
                suddenDeathTimer = Math.max(suddenDeathTimer - 1, 0);
            }
        } else {
            suddenDeathCycleTick = (suddenDeathCycleTick + 1) % SUDDEN_DEATH_CYCLE_TICKS;
        }
    }
}