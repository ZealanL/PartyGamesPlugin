package zealan.pgp.game.games;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.bossbar.BossBarContent;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BlockRange;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;
import zealan.pgp.util.WorldUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class GameAnvilSpleef extends Game {
    public static final GameVariant VARIANT_FAST_ANVILS =
            new GameVariant("fast_anvils", "Fast Anvils", "Anvils fall much faster!", Material.PISTON_BASE);

    public static final BlockRange FLOOR_AREA = BlockRange.fromCorners(
            -263, 0, -3628,
            -223, 0, -3588
    );
    public static final int ANVIL_SPAWN_HEIGHT = 25;
    public static final BlockRange SPAWN_AREA = FLOOR_AREA
            .expand(new Vec3i(2, 0, 2))
            .shift(new Vec3i(0, ANVIL_SPAWN_HEIGHT, 0));

    public static final int ANVIL_SPAWN_INTERVAL = 10;
    public static final int ANVIL_TARGET_SPAWN_MIN_TICKS = 40;
    public static final int ANVIL_TARGET_SPAWN_INTERVAL = 40;
    public static final double ANVIL_TARGET_MAX_MOVE_DIST = 1.0;
    private final HashMap<Gamer, Vec3i> lastPlayerTargetPos = new HashMap<>();

    private final ArrayList<Vec3i> remainingSpawns;
    private int remainingFloorBlocks = FLOOR_AREA.totalBlocks();

    public GameAnvilSpleef(InitParams params) {
        super(params);

        remainingSpawns = new ArrayList<>();
        for (var pos : SPAWN_AREA)
            remainingSpawns.add(pos);
    }

    @Override
    public BossBarContent updateBossBar(Gamer gamer) {
        return new BossBarContent(
                "Blocks left: &b" + remainingFloorBlocks,
                (float)remainingFloorBlocks / FLOOR_AREA.totalBlocks()
        );
    }

    private void spawnAnvil(Vec3i pos) {
        var loc = new Location(world, pos.x, pos.y, pos.z);
        Material material = Material.ANVIL;
        FallingBlock fallingBlock = world.spawnFallingBlock(loc, material, (byte)0);
        //fallingBlock.setDropItem(false);
        fallingBlock.setHurtEntities(true);

        if (variant == VARIANT_FAST_ANVILS)
            fallingBlock.setVelocity(new Vector(0, -1, 0));
    }

    @Override
    protected void innerOnStart() {

    }

    @Override
    protected void innerOnTick(boolean hasStarted) {
        if (!hasStarted)
            return;

        var fallingAnvils = world.getEntitiesByClass(FallingBlock.class).stream().filter(p -> {
            return p.getMaterial() == Material.ANVIL;
        }).toList();

        for (var gamer : getPlayingGamers()) {
            var hitbox = EntityUtil.getEntityHitbox(gamer.player);
            for (var anvil : fallingAnvils) {
                var anvilPos = anvil.getLocation().toVector();
                // TODO: Improve
                if (hitbox.contains(anvilPos) && anvilPos.getY() > gamer.player.getEyeLocation().getY()) {
                    gamer.player.damage(999);
                    world.playSound(anvil.getLocation(), Sound.ANVIL_LAND, 1.0f, 2.0f);
                    Display.sendMsg(world, "&c{} was crushed by a falling anvil!", gamer.player);
                    break;
                }

                // Force send packet to clients
                anvil.teleport(anvil.getLocation());
            }
        }

        // Spawn anvils
        {
            // Possibly target players
            // NOTE: Important to do this BEFORE we spawn 8 anvils to prevent duplicate spawns
            if ((getTicksElapsed() >= ANVIL_TARGET_SPAWN_MIN_TICKS) &&
                    (getTicksElapsed() % ANVIL_TARGET_SPAWN_INTERVAL == 0)) {

                for (var gp : getPlayingGamers()) {
                    var targetBlockPos = EntityUtil.getSupportingBlockPos(gp.player);
                    targetBlockPos = FLOOR_AREA.clampWithin(targetBlockPos); // Prevent it from being outside the floor area

                    var prevBlockPos = lastPlayerTargetPos.get(gp);
                    if (prevBlockPos != null) {
                        var distMoved = prevBlockPos.getBottomCenter().distance(targetBlockPos.getBottomCenter());
                        if (distMoved <= ANVIL_TARGET_MAX_MOVE_DIST) {
                            var targetSpawnBlockPos = targetBlockPos.withY(ANVIL_SPAWN_HEIGHT);
                            spawnAnvil(targetSpawnBlockPos);
                        }
                    }

                    lastPlayerTargetPos.put(gp, targetBlockPos);
                }
            }

            if (getTicksElapsed() % ANVIL_SPAWN_INTERVAL == 0) {
                for (int i = 0; i < 8 && !remainingSpawns.isEmpty(); i++) {
                    var idx = rand.nextInt(remainingSpawns.size());
                    var spawnPos = remainingSpawns.get(idx);
                    spawnAnvil(spawnPos);

                    remainingSpawns.removeIf(
                            bp -> bp.equals(spawnPos)
                    );
                }
            }
        }

        // Make anvils break floor
        for (Vec3i floorPos : FLOOR_AREA) {
            Block floorBlock = WorldUtil.getBlock(world, floorPos);
            if (WorldUtil.getBlock(world, floorPos).isEmpty())
                continue;

            if (WorldUtil.getBlock(world, floorPos.up()).getType() == Material.ANVIL) {
                floorBlock.setType(Material.AIR);
                remainingFloorBlocks--;
            }
        }
    }

    @Override
    protected void innerOnEnd() {

    }
}
