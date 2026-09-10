package zealan.pgp.game.games;

import net.minecraft.server.v1_8_R3.EntityChicken;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftChicken;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.bossbar.BossBarContent;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BlockRange;
import zealan.pgp.math.MoveInput3;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;
import zealan.pgp.util.PlayerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static zealan.pgp.Globals.PLAYER_MGR;
import static zealan.pgp.Globals.PLUGIN;

public class GameChickenRings extends Game {
    public static final GameVariant MAX_SPEED_VARIANT = new GameVariant(
            "max_speed", "Max Speed",
            "Your chicken is constantly at maximum speed!",
            Material.FIREBALL
    );

    public static final double[] SPEED_LEVELS = {
            9.0,
            9.0,
            12.0,
            17.0,
            20.0,
            25.0,
            30.0,
    };
    public static final double SPEED_H_DOWN_MUL = 0.5;
    public static final double SPEED_H_UP_MUL = 1.5;
    public static final double SPEED_VERTICAL = 9.0;

    public final Vector SPAWN_RIGHTMOST_POS = new Vector(2492, 72, 220);

    private static class ChickenController {
        public static final double MOVEMENT_LERP_H = 0.2;

        final Player player;
        final Chicken chicken;
        Vector curMoveDelta = new Vector(0, 0, 0);
        Vector curPos;

        int speedLevelIdx = 0;
        int ringsPassed = 0;

        double getCurSpeedH() {
            return SPEED_LEVELS[Math.min(speedLevelIdx, SPEED_LEVELS.length - 1)];
        }

        private ChickenController(Player player, Chicken chicken, Vector spawnPos) {
            this.player = player;
            this.chicken = chicken;
            this.curPos = spawnPos;
        }

        void updateMovement(Vector movementInput, boolean allowedMove) {
            if (allowedMove) {
                Vector horizontal = new Vector(movementInput.getX(), 0, movementInput.getZ());
                if (horizontal.lengthSquared() > 0) {
                    horizontal.normalize();
                }
                movementInput = horizontal.setY(movementInput.getY());

                double curSpeedH = getCurSpeedH();
                if (movementInput.getY() < 0) {
                    curSpeedH *= SPEED_H_DOWN_MUL;
                } else if (movementInput.getY() > 0) {
                    curSpeedH *= SPEED_H_UP_MUL;
                }

                Vector targetMoveDelta = new Vector(
                        movementInput.getX() * curSpeedH,
                        movementInput.getY() * SPEED_VERTICAL,
                        movementInput.getZ() * curSpeedH
                ).multiply(1.0 / 20.0);

                double lerp = MOVEMENT_LERP_H * (2.0 / 3.0) * (2.0 / 3.0);
                curMoveDelta = new Vector(
                        (curMoveDelta.getX() * (1 - lerp)) + (targetMoveDelta.getX() * lerp),
                        targetMoveDelta.getY(), // Up/down movement has no lerp
                        (curMoveDelta.getZ() * (1 - lerp)) + (targetMoveDelta.getZ() * lerp)
                );
            }

            Location loc = chicken.getLocation();
            float yaw = player.getLocation().getYaw();
            loc.setYaw(yaw);
            loc.setPitch(0);

            EntityChicken nmsChicken = ((CraftChicken) chicken).getHandle();
            nmsChicken.setPosition(this.curPos.getX(), this.curPos.getY(), this.curPos.getZ());
            if (allowedMove) {
                Location fromLoc = chicken.getLocation();
                nmsChicken.move(curMoveDelta.getX(), curMoveDelta.getY(), curMoveDelta.getZ());
                this.curPos = new Vector(nmsChicken.locX, nmsChicken.locY, nmsChicken.locZ);
                curMoveDelta = this.curPos.clone().subtract(fromLoc.toVector());
            }

            EntityUtil.setYaw(chicken, yaw);
            EntityUtil.setHeadYaw(chicken, yaw);
        }
    }

    private static final int RINGS_RADIUS_BLOCKS = 3;
    private static final Vec3i[] RINGS_BORDER_BLOCK_OFFSETS_REL = {
            new Vec3i(-1,  3, 0), new Vec3i( 0,  3, 0), new Vec3i( 1,  3, 0), new Vec3i(-2,  2, 0),
            new Vec3i(-3, -1, 0), new Vec3i(-3,  0, 0), new Vec3i(-3,  1, 0), new Vec3i(-2, -2, 0),
            new Vec3i( 1, -3, 0), new Vec3i( 0, -3, 0), new Vec3i(-1, -3, 0), new Vec3i( 2, -2, 0),
            new Vec3i( 3,  1, 0), new Vec3i( 3,  0, 0), new Vec3i( 3, -1, 0), new Vec3i( 2,  2, 0)
    };

    private static final int NUM_RINGS = 32;
    private static final Vec3i FIRST_RING_POS = new Vec3i(2504, 74 + 3, 194);
    private static final Vec3i RING_POS_VARIANCE_RANGE = new Vec3i(10, 4, 0);
    private static final int RING_Z_SPACING = -25;
    private static final int FINISH_LINE_Z = -594;

    private static final BlockRange PLAYABLE_BOUNDS = BlockRange.fromCorners(
            FIRST_RING_POS.x - 50, 62, 228,
            FIRST_RING_POS.x + 50, 92, FINISH_LINE_Z - 50
    );
    private static final int PLAYABLE_BOUNDS_CHECK_INTERVAL = 2 * 20;

    private final HashMap<Gamer, ChickenController> chickenControllers = new HashMap<>();
    private final List<Vec3i> ringCenters = new ArrayList<>();

    public GameChickenRings(InitParams params) {
        super(params);
    }

    private int nextIntBetweenInclusive(int min, int max) {
        if (min == max) return min;
        return rand.nextInt(max - min + 1) + min;
    }

    private void generateRings() {
        final int varX = RING_POS_VARIANCE_RANGE.x;
        final int varY = RING_POS_VARIANCE_RANGE.y;
        final int varZ = RING_POS_VARIANCE_RANGE.z;

        ringCenters.clear();
        for (int i = 0; i < NUM_RINGS; i++) {
            Vec3i pos = FIRST_RING_POS.add(0, 0, RING_Z_SPACING * i);
            pos = pos.add(
                    nextIntBetweenInclusive(-varX, varX),
                    nextIntBetweenInclusive(-varY, varY),
                    nextIntBetweenInclusive(-varZ, varZ)
            );

            ringCenters.add(pos);
            for (var blockOffset : RINGS_BORDER_BLOCK_OFFSETS_REL) {
                Vec3i blockPos = pos.add(blockOffset);
                var block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
                block.setType(Material.WOOL);
                block.setData((byte) 7); // GRAY_WOOL
            }
        }
    }

    private void setRingBlocksForPlayer(Vec3i ringCenterPos, Player player, Material material, byte data) {
        for (var blockOffset : RINGS_BORDER_BLOCK_OFFSETS_REL) {
            Vec3i blockPos = ringCenterPos.add(blockOffset);
            player.sendBlockChange(
                    new Location(world, blockPos.x, blockPos.y, blockPos.z),
                    material,
                    data
            );
        }
    }

    @Override
    public BossBarContent updateBossBar(Gamer gamer) {
        var controller = chickenControllers.get(gamer);
        if (controller != null) {
            String name = "&rRings completed: &a" + controller.ringsPassed + "&r/" + NUM_RINGS;
            float progress = controller.ringsPassed / (float) NUM_RINGS;
            return new BossBarContent(name, progress);
        }
        return super.updateBossBar(gamer);
    }

    @Override
    public void onLoaded() {
        super.onLoaded();

        var playingGamers = getPlayingGamers();
        for (int i = 0; i < playingGamers.size(); i++) {
            var gamer = playingGamers.get(i);
            Vector ridePos = SPAWN_RIGHTMOST_POS.clone().subtract(new Vector(-i, 0, 0));
            Location spawnLoc = new Location(world, ridePos.getX(), ridePos.getY(), ridePos.getZ(), -180, 0);
            if (!spawnLoc.getChunk().isLoaded())
                    spawnLoc.getChunk().load();
            Chicken chicken = (Chicken) world.spawnEntity(spawnLoc, EntityType.CHICKEN);
            chicken.setRemoveWhenFarAway(false);
            var controller = new ChickenController(gamer.player, chicken, spawnLoc.toVector());
            chickenControllers.put(gamer, controller);

            Bukkit.getScheduler().runTaskLater(PLUGIN, () -> {
                        PlayerUtil.syncMount(gamer.player, chicken);
                    }, 1
            );

        }
        generateRings();
    }

    @Override
    protected void innerOnStart() {
        for (var controller : chickenControllers.values()) {
            setRingBlocksForPlayer(ringCenters.get(0), controller.player, Material.WOOL, (byte) 5); // LIME_WOOL
        }
    }

    @Override
    protected void innerOnEnd() {
        // Remove rings
        for (var ringCenter : ringCenters) {
            for (var blockOffset : RINGS_BORDER_BLOCK_OFFSETS_REL) {
                Vec3i blockPos = ringCenter.add(blockOffset);
                world.getBlockAt(blockPos.x, blockPos.y, blockPos.z).setType(Material.AIR);
            }
        }

        // Clean up chickens
        for (var controller : chickenControllers.values()) {
            if (controller.chicken != null && controller.chicken.isValid()) {
                controller.chicken.remove();
            }
        }
        chickenControllers.clear();
    }

    @Override
    protected void innerOnTick() {
        var playingGamers = getPlayingGamers();
        for (var gamer : playingGamers) {
            var controller = chickenControllers.get(gamer);
            if (controller == null) continue;

            MoveInput3 movementInput = PLAYER_MGR.getMoveInput(gamer.player);
            controller.updateMovement(
                    movementInput.toWorldVector(gamer.player.getLocation().getYaw()), hasStarted()
            );

            Vec3i nextRingPos = ringCenters.get(Math.min(controller.ringsPassed, NUM_RINGS - 1));
            Location chickenLoc = controller.chicken.getLocation();
            Vec3i chickenBlockPos = new Vec3i(chickenLoc.getBlockX(), chickenLoc.getBlockY(), chickenLoc.getBlockZ());

            if (controller.ringsPassed < NUM_RINGS) {
                int distManhattanX = Math.abs(chickenBlockPos.x - nextRingPos.x);
                int distManhattanY = Math.abs(chickenBlockPos.y - nextRingPos.y);
                int ringDistNoZ = distManhattanX + distManhattanY;

                boolean inRing = chickenBlockPos.z == nextRingPos.z && (ringDistNoZ <= RINGS_RADIUS_BLOCKS);
                boolean passed = inRing || (chickenBlockPos.z < nextRingPos.z);

                if (passed) {
                    controller.ringsPassed++;
                    boolean missedRing = ringDistNoZ > RINGS_RADIUS_BLOCKS;

                    // Update ring blocks
                    byte prevWoolData;
                    if (!missedRing) {
                        controller.speedLevelIdx++;
                        prevWoolData = 13; // GREEN_WOOL
                    } else {
                        controller.speedLevelIdx = 0;
                        prevWoolData = 14; // RED_WOOL
                    }

                    setRingBlocksForPlayer(ringCenters.get(controller.ringsPassed - 1), controller.player, Material.WOOL, prevWoolData);
                    if (controller.ringsPassed < NUM_RINGS)
                        setRingBlocksForPlayer(ringCenters.get(controller.ringsPassed), controller.player, Material.WOOL, (byte) 5); // LIME_WOOL

                    int ringNumber = controller.ringsPassed;
                    if (!missedRing) {
                        Display.sendPopupText(gamer.player, "&aRing #" + ringNumber + " passed!");
                        gamer.player.playSound(gamer.player.getLocation(), Sound.ORB_PICKUP, 0.5f, 1.0f);

                        Location partiLoc = new Location(world, nextRingPos.x + 0.5, nextRingPos.y + 0.5, nextRingPos.z + 0.5);
                        world.spigot().playEffect(
                                partiLoc, Effect.LAVA_POP,
                                0, 0,
                                2.0f, 2.0f, 2.0f,
                                0.2f, 50, 64
                        );
                    } else {
                        Display.sendPopupText(gamer.player, "&cRing #" + ringNumber + " missed!");
                        gamer.player.playSound(gamer.player.getLocation(), Sound.HURT_FLESH, 0.8f, 0.75f);
                    }
                } else {
                    // Show particles
                    if (getTicksElapsed() % 8 == 0) {
                        Location partiLoc = new Location(world, nextRingPos.x + 0.5, nextRingPos.y + 0.5, nextRingPos.z + 0.5);
                        world.spigot().playEffect(
                                partiLoc, Effect.HAPPY_VILLAGER,
                                0, 0,
                                1.5f, 1.5f, 1.5f,
                                0.1f, 40, 64
                        );
                    }
                }
            } else {
                if (controller.chicken.getLocation().getZ() <= FINISH_LINE_Z) {
                    Display.sendPopupText(gamer.player, "&a&l&oFinished!");
                    gamer.player.playSound(gamer.player.getLocation(), Sound.LEVEL_UP, 0.9f, 1.5f);
                    gamer.stopPlaying(true);
                }
            }

            if (variant == MAX_SPEED_VARIANT) {
                controller.speedLevelIdx = SPEED_LEVELS.length - 1;
            }

            // Chicken particles and screaming chicken sounds
            if (getTicksElapsed() % 2 == 0) {
                Vector horizVel = new Vector(controller.curMoveDelta.getX(), 0, controller.curMoveDelta.getZ());
                double speedH = horizVel.length();

                if (speedH > 12.0 / 20.0) {
                    Location partiLoc = controller.chicken.getLocation().clone().subtract(
                            controller.curMoveDelta.clone().multiply(3.0)
                    );

                    world.spigot().playEffect(
                            partiLoc, Effect.LAVA_POP,
                            0, 0,
                            0.0f, 0.0f, 0.0f,
                            0.2f, 2, 64
                    );

                    if (getTicksElapsed() % 10 == 0 && rand.nextDouble() < 0.8) {
                        world.playSound(
                                partiLoc,
                                Sound.CHICKEN_HURT,
                                2.0f,
                                (float) rand.nextDouble()
                        );
                    }
                }
            }

            // Out-of-bounds detection
            if (getTicksElapsed() % PLAYABLE_BOUNDS_CHECK_INTERVAL == 0) {
                if (controller.curMoveDelta.lengthSquared() > 1e-6) {
                    if (!PLAYABLE_BOUNDS.contains(chickenBlockPos)) {
                        Vec3i resetVec = (controller.ringsPassed > 0) ?
                                nextRingPos.add(0, -3, -1) :
                                new Vec3i(SPAWN_RIGHTMOST_POS.getBlockX(), SPAWN_RIGHTMOST_POS.getBlockY(), SPAWN_RIGHTMOST_POS.getBlockZ());

                        Location resetLoc = new Location(
                                world, resetVec.x + 0.5, resetVec.y, resetVec.z + 0.5, 0f, 0f
                        );

                        controller.chicken.teleport(resetLoc);
                        gamer.player.teleport(resetLoc);

                        PlayerUtil.syncMount(gamer.player, controller.chicken);

                        controller.curPos = resetLoc.toVector();

                        world.playSound(chickenLoc, Sound.ENDERMAN_TELEPORT, 1.0f, 1.2f);
                        world.playSound(resetLoc, Sound.ENDERMAN_TELEPORT, 2.0f, 1.1f);
                    }
                }
            }
        }
    }

    @Override
    public boolean canDismount(Player player, Entity entity) {
        return player.getGameMode() == GameMode.CREATIVE;
    }
}