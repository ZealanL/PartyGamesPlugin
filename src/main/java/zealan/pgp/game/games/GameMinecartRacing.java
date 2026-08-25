package zealan.pgp.game.games;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftArrow;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftMinecart;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.bossbar.BossBarContent;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BlockRange;
import zealan.pgp.math.BoundingBox;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.WorldUtil;

import java.util.HashMap;

import static zealan.pgp.Globals.PLAYER_MGR;
import static zealan.pgp.Globals.PLOG;

public class GameMinecartRacing extends Game {
    private static final int FINISH_LINE_Z = -1660;
    private static final int MINECART_SPAWN_Z = -2073;
    private static final int MINECART_Y = 49;
    private static final int PLAYER_SPAWN_Z = -2074;

    private static final BlockRange WOOLS_BLOCK_RANGE = BlockRange.fromCorners(
            -1709, 54, -2065,
            -1723, 56, -1670
    );

    private static final byte[] TEAM_WOOL_DATA = {
            DyeColor.WHITE.getWoolData(),
            DyeColor.RED.getWoolData(),
            DyeColor.PINK.getWoolData(),
            DyeColor.LIME.getWoolData(),
            DyeColor.YELLOW.getWoolData(),
            DyeColor.BLUE.getWoolData(),
            DyeColor.PURPLE.getWoolData(),
            DyeColor.ORANGE.getWoolData()
    };

    private static final double MINECART_START_FORWARD_SPEED = 4.0;
    private static final double MINECART_MAX_SPEED = 40.0;
    private static final double MINECART_TICK_DRAG = 0.003;
    private static final double MINECART_BOOST_BONUS = 0.357; // +35.7% speed
    private static final double MINECART_MIN_BOOSTED_SPEED = 2.45;

    private static class MinecartController {
        final Minecart ent;
        double curSpeed;

        MinecartController(Minecart ent) {
            this.ent = ent;
            this.curSpeed = 0.0;
        }
    }

    private final HashMap<Gamer, MinecartController> minecarts = new HashMap<>();

    public GameMinecartRacing(InitParams params) {
        super(params);
    }

    @Override
    public void onLoaded() {
        super.onLoaded();

        // Replace white wool in range with random team wool
        for (Vec3i blockPos : WOOLS_BLOCK_RANGE) {
            Block block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
            if (block.getType() == Material.SPONGE) {
                block.setType(Material.WOOL);
                block.setData(TEAM_WOOL_DATA[rand.nextInt(TEAM_WOOL_DATA.length)]);
            }
        }

        for (Gamer gamer : getGamers()) {
            byte woolData = TEAM_WOOL_DATA[gamer.spawnIdx % TEAM_WOOL_DATA.length];
            ItemStack woolStack = new ItemStack(Material.WOOL, 1, woolData);
            for (int i = 3; i <= 6; i++) {
                gamer.player.getInventory().setItem(i, woolStack);
            }
        }
    }

    @Override
    protected void innerOnStart() {
        for (Gamer gamer : getGamers()) {
            // Give bow with infinity
            ItemStack bowStack = new ItemStack(Material.BOW);
            bowStack.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);

            gamer.player.getInventory().setItem(0, bowStack);
            gamer.player.getInventory().setItem(1, new ItemStack(Material.ARROW));

            Location spawnLoc = getGamerSpawn(gamer).toLocation(world);
            Location minecartSpawnLoc = new Location(
                    world,
                    spawnLoc.getBlockX() + 0.5,
                    MINECART_Y,
                    MINECART_SPAWN_Z + 0.5,
                    0, 0
            );

            Minecart minecart = world.spawn(minecartSpawnLoc, Minecart.class);
            minecart.setMaxSpeed(999.0);
            minecart.setPassenger(gamer.player);

            minecarts.put(gamer, new MinecartController(minecart));
        }
    }

    @Override
    protected void innerOnEnd() {
        for (MinecartController controller : minecarts.values()) {
            if (controller.ent != null && controller.ent.isValid()) {
                controller.ent.remove();
            }
        }
        minecarts.clear();
    }

    @Override
    public BossBarContent updateBossBar(Gamer gamer) {
        MinecartController minecart = minecarts.get(gamer);
        if (minecart != null) {
            double progress = (gamer.player.getLocation().getZ() - PLAYER_SPAWN_Z) / (double) (FINISH_LINE_Z - PLAYER_SPAWN_Z);
            return new BossBarContent(
                    Display.format(String.format("&bSpeed: &6&l%.2f blocks/s", minecart.curSpeed)),
                    Math.max(0.0, Math.min(1.0, progress))
            );
        } else {
            return super.updateBossBar(gamer);
        }
    }

    @Override
    protected void innerOnTick() {
        for (Gamer gamer : getPlayingGamers()) {
            MinecartController minecart = minecarts.get(gamer);
            if (minecart == null || !minecart.ent.isValid())
                continue;

            if (gamer.player.getVehicle() != minecart.ent) {
                if (gamer.player.getGameMode() != GameMode.CREATIVE)
                    minecart.ent.setPassenger(gamer.player);
            }

            var inputs = PLAYER_MGR.getMoveInput(gamer.player);
            if (Math.abs(minecart.curSpeed) < 1e-3 && inputs.forward > 0) {
                minecart.curSpeed = MINECART_START_FORWARD_SPEED;
            }

            if (minecart.curSpeed >= MINECART_MAX_SPEED) {
                minecart.curSpeed = MINECART_MAX_SPEED;
            } else {
                minecart.curSpeed *= (1.0 - MINECART_TICK_DRAG);
            }
            minecart.ent.setVelocity(new Vector(0, 0, minecart.curSpeed / 20.0));

            if (gamer.player.getLocation().getZ() >= FINISH_LINE_Z) {
                gamer.stopPlaying(true);
                if (gamer.player.getVehicle() != null) {
                    gamer.player.getVehicle().eject();
                }
                gamer.player.playSound(gamer.player.getLocation(), Sound.LEVEL_UP, 0.9f, 1.0f);
            }
        }

        var arrows = world.getEntitiesByClass(Arrow.class);
        for (var arrow : arrows) {
            var nmsArrow = ((CraftArrow) arrow).getHandle();
            if (nmsArrow.inGround) {
                Vector direction = arrow.getVelocity().normalize();
                var forwardLoc = arrow.getLocation();
                if (forwardLoc.getBlock().isEmpty())
                    forwardLoc = forwardLoc.add(direction.multiply(0.2));

                var hitBlock = forwardLoc.getBlock();

                if (hitBlock.getType() == Material.WOOL) {
                    byte hitWoolData = hitBlock.getData();
                    for (Gamer gamer : getPlayingGamers()) {
                        byte teamWoolData = TEAM_WOOL_DATA[gamer.spawnIdx % TEAM_WOOL_DATA.length];
                        if (hitWoolData == teamWoolData) {
                            MinecartController minecartController = minecarts.get(gamer);
                            if (minecartController != null) {
                                minecartController.curSpeed = Math.max(
                                        MINECART_MIN_BOOSTED_SPEED,
                                        minecartController.curSpeed * (1.0 + MINECART_BOOST_BONUS)
                                );
                                gamer.player.playSound(gamer.player.getLocation(), Sound.SUCCESSFUL_HIT, 0.9f, 1.0f);
                                world.playEffect(gamer.player.getLocation(), Effect.LAVA_POP, 4);
                            }
                            break;
                        }
                    }

                    WorldUtil.breakBlock(world, hitBlock);
                }

                arrow.remove();
            }
        }
    }

    @Override
    public boolean canUseItem(Player player, ItemStack item) {
        return item.getType() == Material.BOW;
    }

    @Override
    public boolean canInteractBlock(Player player, Block block) {
        return true;
    }
}