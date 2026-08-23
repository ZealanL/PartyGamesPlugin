package zealan.pgp.game.games;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftSheep;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.MoveInput3;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;
import zealan.pgp.util.PlayerUtil;

import static zealan.pgp.Globals.PLAYER_MGR;
import static zealan.pgp.Globals.PLUGIN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class GameSuperSheep extends Game {
    public GameSuperSheep(InitParams params) {
        super(params);
    }

    private record TeamColorSet(DyeColor dye, Material glassPane, byte glassPaneData) {}

    private static final TeamColorSet[] TEAM_COLOR_SETS = {
            new TeamColorSet(DyeColor.BROWN, Material.STAINED_GLASS_PANE, DyeColor.BROWN.getWoolData()),
            new TeamColorSet(DyeColor.MAGENTA, Material.STAINED_GLASS_PANE, DyeColor.MAGENTA.getWoolData()),
            new TeamColorSet(DyeColor.YELLOW, Material.STAINED_GLASS_PANE, DyeColor.YELLOW.getWoolData()),
            new TeamColorSet(DyeColor.ORANGE, Material.STAINED_GLASS_PANE, DyeColor.ORANGE.getWoolData()),
            new TeamColorSet(DyeColor.LIME, Material.STAINED_GLASS_PANE, DyeColor.LIME.getWoolData()),
            new TeamColorSet(DyeColor.BLUE, Material.STAINED_GLASS_PANE, DyeColor.BLUE.getWoolData()),
            new TeamColorSet(DyeColor.LIGHT_BLUE, Material.STAINED_GLASS_PANE, DyeColor.LIGHT_BLUE.getWoolData()),
            new TeamColorSet(DyeColor.WHITE, Material.STAINED_GLASS_PANE, DyeColor.WHITE.getWoolData()),
    };

    private class SheepController {
        private static final double MOVEMENT_LERP_H = 0.2; // TODO: Unknown
        private static final int MOVEMENT_UPDATE_INTERVAL = 2;
        private static final int MAX_GAS = 50;
        private static final int GAS_USAGE_BOOSTING = 3;

        private static final double BASE_SPEED = 6.5;
        private static final double BOOSTING_SPEED_MUL = 2.0;
        private static final double HOLDING_S_SPEED_BONUS = 2.0;
        private static final double JUMP_BOOST_Y = 3.0;
        private static final double FLOOR_Y = 2.0;

        final Gamer gamer;
        final Sheep sheep;
        Vector curMoveDelta = new Vector(0, 0, 0);
        int ticksElapsed = 0;
        Vec3i lastBlockPos = null;

        int gas = MAX_GAS;
        double curJumpY = 0;
        int curJumpTimer = 0;

        private SheepController(Gamer gamer, Sheep sheep) {
            this.gamer = gamer;
            this.sheep = sheep;
        }

        void updateMovement(MoveInput3 input2, boolean jumpInput, boolean canMove) {
            if (ticksElapsed % MOVEMENT_UPDATE_INTERVAL == 0 && canMove) {
                float moveYaw = gamer.player.getLocation().getYaw();
                double moveSpeed = BASE_SPEED;

                double gasFrac = (double) gas / MAX_GAS;
                boolean boostingForward = input2.forward > 0;
                boolean boostingSideways = input2.sideways != 0;
                if (boostingForward || boostingSideways || jumpInput) {
                    if (gas > 0) {
                        moveSpeed *= BOOSTING_SPEED_MUL;
                        if (boostingSideways) {
                            if (input2.sideways < 0) {
                                moveYaw += 45;
                            } else {
                                moveYaw -= 45;
                            }
                        }

                        gas = Math.max(0, gas - GAS_USAGE_BOOSTING);

                        gamer.player.playSound(
                                gamer.player.getLocation(), Sound.NOTE_BASS, 1.0f,
                                (float) (1.0 + (1.0 - gasFrac))
                        );
                    } else {
                        Display.sendPopupText(gamer.player, "&c&oOut of gas!");
                    }
                } else {
                    gas = Math.min(gas + 1, MAX_GAS);
                }
                {
                    gamer.player.setLevel(0);
                    gamer.player.setExp((float) gasFrac);
                }

                if (input2.forward < 0) {
                    moveSpeed += HOLDING_S_SPEED_BONUS;
                }

                Vector targetMoveDelta = new MoveInput3(1, 0, 0)
                        .toWorldVector(moveYaw)
                        .multiply(moveSpeed)
                        .multiply(1.0 / 20.0);
                double lerp = MOVEMENT_LERP_H;

                curMoveDelta = new Vector(
                        (curMoveDelta.getX() * (1 - lerp)) + (targetMoveDelta.getX() * lerp),
                        0,
                        (curMoveDelta.getZ() * (1 - lerp)) + (targetMoveDelta.getZ() * lerp)
                );
            }

            ((CraftEntity) sheep).getHandle().move(curMoveDelta.getX(), 0.0, curMoveDelta.getZ());
            ((CraftEntity) sheep).getHandle().yaw = gamer.player.getLocation().getYaw();


            // TODO: This is a rough approximation of the real system
            {
                boolean tryStartJump = jumpInput && gas > 0;
                if (tryStartJump && curJumpY == 0)
                    curJumpTimer = 6;

                if (curJumpTimer > 0) {
                    if (curJumpTimer >= 3) {
                        curJumpY += 0.34375;
                    } else {
                        curJumpY -= 0.3;
                    }
                    curJumpTimer--;
                } else {
                    curJumpY = 0;
                }

                var sheepPos = sheep.getLocation().toVector();
                ((CraftEntity) sheep).getHandle().setPosition(sheepPos.getX(), FLOOR_Y + curJumpY, sheepPos.getZ());
            }

            ticksElapsed++;
        }
    }

    private static final int TRAIL_LIMIT_START = 17;
    private static final int TRAIL_LIMIT_INC_INTERVAL = 10;

    // ///////////

    private final HashMap<Gamer, SheepController> sheepControllers = new HashMap<>();
    private final HashMap<SheepController, LinkedList<Vec3i>> trails = new HashMap<>();

    @Override
    public void onLoaded() {
        super.onLoaded();

        var playingGamers = getPlayingGamers();
        for (Gamer gp : playingGamers) {
            spawnGamer(gp);

            Location spawnLoc = gp.player.getLocation();
            Sheep sheep = (Sheep) world.spawnEntity(spawnLoc, EntityType.SHEEP);

            sheep.setColor(TEAM_COLOR_SETS[gp.spawnIdx].dye());

            // Mount is synced a tick later via the shared PlayerUtil helper (confirmed pattern
            // from GameLawnMoower) rather than calling setPassenger immediately, since the client
            // needs a tick to register the freshly-spawned vehicle entity first.
            Bukkit.getScheduler().runTaskLater(
                    PLUGIN,
                    () -> PlayerUtil.syncMount(gp.player, sheep),
                    1
            );

            SheepController controller = new SheepController(gp, sheep);
            sheepControllers.put(gp, controller);
        }
    }

    @Override
    protected void innerOnStart() {
        for (Gamer gamer : getPlayingGamers()) {
            SheepController controller = sheepControllers.get(gamer);
            PlayerUtil.syncMount(gamer.player, controller.sheep);
        }
    }

    @Override
    protected void innerOnEnd() {
        for (var controller : sheepControllers.values()) {
            if (controller.sheep != null && controller.sheep.isValid())
                controller.sheep.remove();
        }
        if (hasStarted()) {
            for (Gamer gamer : getPlayingGamers()) {
                if (gamer.player != null && gamer.player.isOnline())
                    gamer.player.eject();
            }
        }
        sheepControllers.clear();
        trails.clear();
    }

    @Override
    protected void innerOnTick() {
        for (var gp : getPlayingGamers()) {
            var controller = sheepControllers.get(gp);
            if (controller == null || !controller.sheep.isValid())
                continue;

            MoveInput3 input = PLAYER_MGR.getMoveInput(gp.player);
            controller.updateMovement(input, input.up > 0.0, hasStarted());

            if (EntityUtil.isCollidedHorizontally(controller.sheep)) {
                controller.sheep.damage(696969);
                controller.gamer.player.damage(696969);
                world.playSound(controller.sheep.getLocation(), Sound.EXPLODE, 0.6f, 1.5f);
                world.spigot().playEffect(
                        controller.sheep.getLocation(), Effect.EXPLOSION_LARGE,
                        0, 1,
                        0.3f, 0.3f, 0.3f,
                        0.2f, 5, 64
                );
            }
        }

        if (!hasStarted())
            return;

        // Update trails
        var curTrailLimit = TRAIL_LIMIT_START + (getTicksElapsed() / TRAIL_LIMIT_INC_INTERVAL);
        for (var gp : getPlayingGamers()) {
            var controller = sheepControllers.get(gp);
            if (controller == null || !controller.sheep.isValid())
                continue;

            trails.putIfAbsent(controller, new LinkedList<>());
            var trail = trails.get(controller);

            Location curLoc = controller.sheep.getLocation();
            Vec3i newBlockPos = new Vec3i(curLoc.getBlockX(), curLoc.getBlockY(), curLoc.getBlockZ());
            Vec3i oldBlockPos = controller.lastBlockPos != null ? controller.lastBlockPos : newBlockPos;

            if (!newBlockPos.equals(oldBlockPos)) {
                var blocksToFill = new ArrayList<Vec3i>();
                blocksToFill.add(oldBlockPos);

                int dx = newBlockPos.x - oldBlockPos.x;
                int dz = newBlockPos.z - oldBlockPos.z;
                if (dx != 0 && dz != 0) {
                    blocksToFill.add(new Vec3i(oldBlockPos.x + dx, oldBlockPos.y, oldBlockPos.z));
                    blocksToFill.add(new Vec3i(oldBlockPos.x, oldBlockPos.y, oldBlockPos.z + dz));
                }

                for (Vec3i blockPos : blocksToFill) {
                    Block block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
                    if (block.getType() == Material.AIR) {
                        var colorSet = TEAM_COLOR_SETS[gp.spawnIdx];
                        block.setType(colorSet.glassPane());
                        block.setData(colorSet.glassPaneData());
                        trail.add(blockPos);
                    }
                }
            }
            controller.lastBlockPos = newBlockPos;

            while (trail.size() > curTrailLimit) {
                var removedPos = trail.removeFirst();
                world.getBlockAt(removedPos.x, removedPos.y, removedPos.z).setType(Material.AIR);
            }
        }
    }

    @Override
    public boolean canDismount(Player player, Entity entity) {
        // Confirmed hook (see GameLawnMoower): block dismounting once the game is running.
        return !hasStarted();
    }
}