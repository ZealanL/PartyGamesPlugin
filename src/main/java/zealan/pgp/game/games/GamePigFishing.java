package zealan.pgp.game.games;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.Vec3i;

import java.util.HashMap;
import java.util.HashSet;

public class GamePigFishing extends Game {
    private static final Vector PIG_SPAWN_POS = new Vector(450, 38, -1869);
    private static final int PIG_SPAWN_INTERVAL = 20;
    private static final int IN_PIT_Y_THRESH = 17;
    private static final double BABY_PIG_CHANCE = 0.25;
    private static final int MAX_PIGS = 20;
    private static final int PISTONS_Y = 21;

    private static final int SUPER_BACON_DURATION = 20 * 5;
    private static final double BABY_SUPER_BACON_CHANCE = 0.25; // TODO: Rough guess

    private final HashSet<Pig> pigs = new HashSet<>();
    private final HashMap<Gamer, Integer> superBaconTimers = new HashMap<>();

    public GamePigFishing(InitParams params) {
        super(params);
    }

    @Override
    public void spawnGamer(Gamer gamer) {
        super.spawnGamer(gamer);

        gamer.player.getInventory().setItem(0, new ItemStack(Material.FISHING_ROD));
    }

    private void updatePistons(int spawnIdx, boolean powered) {
        var spawn = config.loadInfo.spawns[spawnIdx];
        Vec3i basePos = spawn.getBlockPos().alongBlockFace(spawn.dir, 4).withY(PISTONS_Y);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Vec3i blockPos = basePos.add(dx, 0, dz);
                Material blockType = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z).getType();
                if (blockType.equals(Material.PISTON_BASE) || blockType.equals(Material.PISTON_STICKY_BASE))
                    world.getBlockAt(blockPos.x, blockPos.y - 1, blockPos.z).setType(powered ? Material.REDSTONE_BLOCK : Material.WOOD);
            }
        }
    }

    @Override
    protected void innerOnStart() {

    }

    @Override
    protected void innerOnTick() {
        if (hasStarted() && getTicksElapsed() % PIG_SPAWN_INTERVAL == 0) {
            if (pigs.size() < MAX_PIGS) {
                Pig pig = (Pig)world.spawnEntity(
                        new Location(world, PIG_SPAWN_POS.getX(), PIG_SPAWN_POS.getY(), PIG_SPAWN_POS.getZ()),
                        EntityType.PIG
                );

                if (rand.nextDouble() < BABY_PIG_CHANCE) {
                    pig.setBaby();
                    if (rand.nextDouble() < BABY_SUPER_BACON_CHANCE) {
                        pig.setCustomName(Display.format("&cSuper Bacon"));
                        pig.setCustomNameVisible(true);
                    }
                } else {
                    pig.setAdult();
                }

                pigs.add(pig);
            }
        }

        var spawns = config.loadInfo.spawns;
        for (var pig : pigs) {
            Vector pigPos = pig.getLocation().toVector();
            if (pigPos.getY() <= IN_PIT_Y_THRESH) {
                int curClosestSpawnIdx = -1;
                double closestSpawnDistSq = Double.MAX_VALUE;
                for (int i = 0; i < spawns.length; i++) {
                    var spawnPos = spawns[i].pos;
                    var sqDist = pigPos.distanceSquared(spawnPos);
                    if (sqDist < closestSpawnDistSq) {
                        curClosestSpawnIdx = i;
                        closestSpawnDistSq = sqDist;
                    }
                }
                final int closestSpawnIdx = curClosestSpawnIdx;

                if (closestSpawnIdx != -1) {
                    var gp = getPlayingGamers().stream().filter(
                            g -> g.spawnIdx == closestSpawnIdx
                    ).findFirst().orElse(null);

                    if (gp != null) {
                        gp.points++;
                        gp.player.playSound(gp.player.getLocation(), Sound.ORB_PICKUP, 0.8f, 1.0f);

                        if (pig.isCustomNameVisible()) {
                            // Activate super bacon
                            Display.sendMsg(world, "{} &fcaptured &cSuper Bacon&f!", gp.player);
                            superBaconTimers.put(gp, SUPER_BACON_DURATION);
                            for (int i = 0; i < config.loadInfo.spawns.length; i++) {
                                if (i == gp.spawnIdx)
                                    continue;

                                updatePistons(i, true);
                            }
                        }
                    }
                }

                pig.remove();
            }

            if (pig.isCustomNameVisible()) {
                world.playEffect(pig.getLocation(), Effect.HAPPY_VILLAGER, 3);
            }
        }
        pigs.removeIf(pig -> !pig.isValid());

        superBaconTimers.replaceAll((k, v) -> v - 1);
        for (var entry : superBaconTimers.entrySet()) {
            if (entry.getValue() == 0) {
                for (int i = 0; i < config.loadInfo.spawns.length; i++) {
                    updatePistons(i, false);
                }
            }
        }
        superBaconTimers.entrySet().removeIf((e) -> e.getValue() <= 0);

        // OBB check
        for (var gamer : getPlayingGamers()) {
            Vector spawnPos = getGamerSpawn(gamer).pos;
            Vector gamerPos = gamer.player.getLocation().toVector();

            Vector offset = gamerPos.subtract(spawnPos);

            BlockFace spawnDir = getGamerSpawn(gamer).dir;
            if (spawnDir.getModX() != 0) {
                offset.setX(0);
            } else if (spawnDir.getModZ() != 0) {
                offset.setZ(0);
            }

            boolean outOfBounds = false;
            if (offset.getY() < -0.1) {
                outOfBounds = true;
            } else {
                if (Math.abs(offset.getY()) > 0.1) {
                    double dist = Math.max(Math.abs(offset.getX()), Math.abs(offset.getZ()));
                    if (dist > 5)
                        spawnGamer(gamer);
                }
            }

            if (outOfBounds)
                spawnGamer(gamer);
        }
    }

    @Override
    protected void innerOnEnd() {

    }

    @Override
    public boolean canInteractBlock(Player player, Block block) {
        return true;
    }

    @Override
    public boolean canUseItem(Player player, ItemStack item) {
        return true;
    }

    @Override
    public boolean canAttackEntity(Player player, Entity entity, EntityDamageByEntityEvent event) {
        return entity instanceof Pig;
    }
}
