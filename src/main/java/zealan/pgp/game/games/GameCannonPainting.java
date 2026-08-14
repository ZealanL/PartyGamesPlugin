package zealan.pgp.game.games;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BlockRange;
import zealan.pgp.math.BoundingBox;
import zealan.pgp.math.Vec3i;

import java.util.HashMap;

import static zealan.pgp.Globals.PLOG;

public class GameCannonPainting extends Game {
    public static final GameVariant VARIANT_NO_COOLDOWN = new GameVariant(
            "no_cooldown", "No Cooldown", "Removes cooldown on egg throwing",
            Material.WATCH
    );

    private static final int PAINT_WOOL_X = 195;
    private static final int FLOOR_Y = 18;
    private static final BlockRange PLAYABLE_RANGE = BlockRange.fromCorners(
            216, 18, 1462,
            225, 25, 1426
    );

    // Wool sub-ids for colors corresponding to 1.21 team wool order
    private static final byte[] TEAM_WOOL_DATA = {
            DyeColor.LIGHT_BLUE.getWoolData(),
            DyeColor.RED.getWoolData(),
            DyeColor.PINK.getWoolData(),
            DyeColor.LIME.getWoolData(),
            DyeColor.BLACK.getWoolData(),
            DyeColor.CYAN.getWoolData(),
            DyeColor.PURPLE.getWoolData(),
            DyeColor.ORANGE.getWoolData()
    };

    private static final int THROW_COOLDOWN_TICKS = 5;

    private final HashMap<Vec3i, Gamer> paintings = new HashMap<>();
    private final HashMap<Gamer, Integer> lastThrowTicks = new HashMap<>();

    public GameCannonPainting(InitParams params) {
        super(params);
    }

    @Override
    protected void innerOnStart() {
        for (Gamer gamer : getPlayingGamers()) {
            gamer.player.getInventory().setItem(0, new ItemStack(Material.EGG, 64));
        }
    }

    @Override
    protected void innerOnEnd() {
    }

    @Override
    protected void innerOnTick() {
        if (!hasStarted())
            return;

        for (Gamer gamer : getPlayingGamers()) {
            ItemStack firstStack = gamer.player.getInventory().getItem(0);
            if (firstStack == null || firstStack.getType() != Material.EGG || firstStack.getAmount() != 64) {
                gamer.player.getInventory().setItem(0, new ItemStack(Material.EGG, 64));
                gamer.player.updateInventory();
            }

            var playerBlockPos = Vec3i.from(gamer.player.getLocation().toVector());
            if (gamer.player.getLocation().getBlockY() < FLOOR_Y || !PLAYABLE_RANGE.contains(playerBlockPos)) {
                spawnGamer(gamer);
            }
        }
    }

    private void changeWool(Vec3i pos, Gamer gamer) {
        Block block = world.getBlockAt(pos.x, pos.y, pos.z);
        if (block.getType() != Material.WOOL)
            return;

        Gamer prevGamer = paintings.get(pos);
        if (prevGamer != null)
            prevGamer.points--;

        paintings.put(pos, gamer);
        gamer.points++;

        byte teamWoolData = TEAM_WOOL_DATA[gamer.spawnIdx % TEAM_WOOL_DATA.length];

        for (Gamer otherGamer : getGamers()) {
            if (otherGamer == gamer) {
                otherGamer.player.sendBlockChange(block.getLocation(), Material.WOOL, DyeColor.YELLOW.getWoolData());
            } else {
                otherGamer.player.sendBlockChange(block.getLocation(), Material.WOOL, teamWoolData);
            }
        }
    }

    @Override
    public boolean canUseItem(Player player, ItemStack item) {
        var gamer = getGamer(player);
        if (gamer == null)
            return false;

        if (item != null && item.getType() == Material.EGG) {
            int lastThrowTick = lastThrowTicks.getOrDefault(gamer, -999);
            int timeSince = gamer.player.getTicksLived() - lastThrowTick;

            if (timeSince >= THROW_COOLDOWN_TICKS || variant == VARIANT_NO_COOLDOWN) {
                lastThrowTicks.put(gamer, gamer.player.getTicksLived());
                return true;
            }
        }
        return false;
    }

    Location extrapProjectileHit(Projectile projectile) {
        var curLoc = projectile.getLocation();
        for (int i = 0; i < 6; i++) {
            if (!world.getBlockAt(curLoc).isEmpty())
                return curLoc;

            curLoc = curLoc.add(projectile.getVelocity().multiply(0.5));
        }
        return curLoc;
    }

    @Override
    public void onProjectileHit(Projectile projectile) {
        var shooter = projectile.getShooter();
        if (shooter instanceof Player player && projectile instanceof Egg egg) {
            var gamer = getGamer(player);
            if (gamer == null)
                return;

            var extrapPos = extrapProjectileHit(egg);
            var hitBlock = world.getBlockAt(extrapPos);

            if (hitBlock.getType() == Material.WOOL && hitBlock.getX() == PAINT_WOOL_X) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Vec3i paintPos = new Vec3i(hitBlock.getX(), hitBlock.getY() + dy, hitBlock.getZ() + dz);
                        changeWool(paintPos, gamer);
                    }
                }
            }
        }
    }
}