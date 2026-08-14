package zealan.pgp.game.games;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.MoveInput3;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;
import zealan.pgp.util.PlayerUtil;
import zealan.pgp.util.WorldUtil;

import java.util.HashMap;

import static zealan.pgp.Globals.PLAYER_MGR;
import static zealan.pgp.Globals.PLUGIN;

public class GameLawnMoower extends Game {
    public static final Vec3i CENTER_BLOCK_POS = new Vec3i(-1071, 47, 715);

    private static final double COW_BASE_SPEED = 6.4 / 20.0;
    private static final double COW_FASTER_SPEED = 8.25 / 20.0;
    private static final double MOO_CHANCE_PER_TICK = 0.005;
    private static final double COW_MOVEMENT_LERP = 0.07;

    public class CowController {
        public final Player player;
        public final Entity vehicle;
        protected Vector prevMovementInput = new Vector(0,0,0);
        public CowController(Player player, Entity vehicle) {
            this.player = player;
            this.vehicle = vehicle;
        }

        public void updateMovement(Vector movementInput, double lerp) {
            movementInput = prevMovementInput.clone().multiply(1.0 - lerp).add(movementInput.clone().multiply(lerp));
            ((CraftEntity)vehicle).getHandle().move(movementInput.getX(), 0.0, movementInput.getZ());
            ((CraftEntity)vehicle).getHandle().yaw = player.getLocation().getYaw();
            prevMovementInput = movementInput;
        }
    }

    private final HashMap<Gamer, CowController> vehicleControllers = new HashMap<>();

    public GameLawnMoower(InitParams params) {
        super(params);
    }

    @Override
    public void onLoaded() {
        super.onLoaded();

        for (Gamer gamer : getPlayingGamers()) {
            spawnGamer(gamer);

            Location loc = gamer.player.getLocation();
            Cow cow = (Cow) world.spawnEntity(loc, EntityType.COW);

            Bukkit.getScheduler().runTaskLater(
                    PLUGIN,
                    () ->  PlayerUtil.syncMount(gamer.player, cow),
                    1
            );

            CowController controller = new CowController(gamer.player, cow);
            vehicleControllers.put(gamer, controller);
        }
    }

    @Override
    protected void innerOnStart() {
        for (Gamer gamer : getPlayingGamers()) {
            CowController controller = vehicleControllers.get(gamer);
            PlayerUtil.syncMount(gamer.player, controller.vehicle);
        }
    }

    @Override
    protected void innerOnEnd() {
        for (Gamer gamer : getPlayingGamers()) {
            CowController controller = vehicleControllers.get(gamer);
            if (controller != null && controller.vehicle != null && controller.vehicle.isValid()) {
                controller.vehicle.remove();
            }
            if (gamer.player != null && gamer.player.isOnline()) {
                gamer.player.eject();
            }
        }
        vehicleControllers.clear();
    }

    @Override
    protected void innerOnTick() {
        if (!hasStarted())
            return;

        for (Gamer gamer : getPlayingGamers()) {
            CowController controller = vehicleControllers.get(gamer);
            if (controller != null && controller.vehicle instanceof Cow cow && cow.isValid()) {
                cow.getLocation().setYaw(gamer.player.getLocation().getYaw());

                float playerYaw = gamer.player.getLocation().getYaw();
                MoveInput3 realPlayerInput = PLAYER_MGR.getMoveInput(gamer.player);
                Vector fakePlayerInput = new MoveInput3(1, 0, 0).toWorldVector(playerYaw);
                boolean moveFaster = realPlayerInput.forward != 0 || realPlayerInput.sideways != 0;
                double cowSpeed = moveFaster ? COW_FASTER_SPEED : COW_BASE_SPEED;

                controller.updateMovement(fakePlayerInput.multiply(cowSpeed), COW_MOVEMENT_LERP);

                Location cowLoc = cow.getLocation();
                Block block = world.getBlockAt(cowLoc.getBlockX(), cowLoc.getBlockY(), cowLoc.getBlockZ());
                var blockType = block.getType();
                if (blockType == Material.DOUBLE_PLANT) {
                    WorldUtil.breakBlock(world, block);
                    gamer.points++;
                }

                if (rand.nextDouble() < MOO_CHANCE_PER_TICK) {
                    world.playSound(cowLoc, Sound.COW_IDLE, 0.8f, 1.0f);
                }
            }
        }
    }

    @Override
    public boolean canDismount(Player player, Entity entity) {
        return !hasStarted();
    }
}
