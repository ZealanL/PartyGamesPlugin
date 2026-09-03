package zealan.pgp.game.games;

import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftSpider;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;
import zealan.pgp.game.GameSpawn;
import zealan.pgp.math.BlockRange;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;

import java.util.HashMap;

import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.GenericAttributes;
import zealan.pgp.util.PlayerUtil;

public class GameSpiderMaze extends Game {
    public static GameVariant VARIANT_SUPER_SPIDERS = new GameVariant(
            "super_spiders", "Super Spiders", "You better not slow down!", Material.SPIDER_EYE
    );

    public GameSpiderMaze(InitParams params) {
        super(params);
    }

    public static final GameSpawn[] SPAWNS = new GameSpawn[]{
            new GameSpawn(new Vector(93, 5, 2037), BlockFace.SOUTH),
            new GameSpawn(new Vector(77, 5, 2037), BlockFace.SOUTH),
            new GameSpawn(new Vector(61, 5, 2037), BlockFace.SOUTH),
            new GameSpawn(new Vector(45, 5, 2037), BlockFace.SOUTH),
            new GameSpawn(new Vector(29, 5, 2037), BlockFace.SOUTH),
            new GameSpawn(new Vector(13, 5, 2037), BlockFace.SOUTH),
            new GameSpawn(new Vector(-3, 5, 2037), BlockFace.SOUTH),

            new GameSpawn(new Vector(-17, 5, 2051), BlockFace.EAST),
            new GameSpawn(new Vector(-17, 5, 2067), BlockFace.EAST),
            new GameSpawn(new Vector(-17, 5, 2083), BlockFace.EAST),
            new GameSpawn(new Vector(-17, 5, 2099), BlockFace.EAST),
            new GameSpawn(new Vector(-17, 5, 2115), BlockFace.EAST),
            new GameSpawn(new Vector(-17, 5, 2131), BlockFace.EAST),
            new GameSpawn(new Vector(-17, 5, 2147), BlockFace.EAST),

            new GameSpawn(new Vector(-3, 5, 2161), BlockFace.NORTH),
            new GameSpawn(new Vector(13, 5, 2161), BlockFace.NORTH),
            new GameSpawn(new Vector(29, 5, 2161), BlockFace.NORTH),
            new GameSpawn(new Vector(45, 5, 2161), BlockFace.NORTH),
            new GameSpawn(new Vector(61, 5, 2161), BlockFace.NORTH),
            new GameSpawn(new Vector(77, 5, 2161), BlockFace.NORTH),
            new GameSpawn(new Vector(93, 5, 2161), BlockFace.NORTH),

            new GameSpawn(new Vector(107, 5, 2147), BlockFace.WEST),
            new GameSpawn(new Vector(107, 5, 2131), BlockFace.WEST),
            new GameSpawn(new Vector(107, 5, 2115), BlockFace.WEST),
            new GameSpawn(new Vector(107, 5, 2099), BlockFace.WEST),
            new GameSpawn(new Vector(107, 5, 2083), BlockFace.WEST),
            new GameSpawn(new Vector(107, 5, 2067), BlockFace.WEST),
            new GameSpawn(new Vector(107, 5, 2051), BlockFace.WEST),
    };

    private static BlockFace getSpawnDirection(int spawnIdx) {
        final BlockFace[] spawnDirs = {
                BlockFace.SOUTH,
                BlockFace.EAST,
                BlockFace.NORTH,
                BlockFace.WEST
        };
        return spawnDirs[spawnIdx / 8];
    }

    private static final int SPIDER_RELEASE_DELAY_TICKS = 3 * 20;
    private final HashMap<Spider, Gamer> spiders = new HashMap<>();

    public static final Vector WIN_POS = new Vector(45, 2, 2099);
    public static final double WIN_RANGE = 1.5;

    @Override
    public void onLoaded() {
        super.onLoaded();

        world.setTime(13000);

        int numSpidersPer = 1;
        if (variant == VARIANT_SUPER_SPIDERS)
            numSpidersPer = 2;
        for (int i = 0; i < numSpidersPer; i++) {
            for (Gamer gp : getPlayingGamers()) {
                GameSpawn spawn = getGamerSpawn(gp);
                final double SPIDER_SPAWN_OFFSET = -5.5;
                Vector spiderSpawn = spawn.getBlockPos().alongBlockFace(spawn.getDir(), SPIDER_SPAWN_OFFSET);
                Location spiderSpawnPos = new Location(
                        world,
                        spiderSpawn.getX(),
                        spiderSpawn.getY(),
                        spiderSpawn.getZ()
                );

                Spider spider = (Spider) world.spawnEntity(spiderSpawnPos, EntityType.SPIDER);

                EntityLiving nmsSpider = ((CraftLivingEntity) spider).getHandle();
                nmsSpider.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(
                        (variant == VARIANT_SUPER_SPIDERS) ? 0.8 : 0.3
                );
                nmsSpider.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(6.0);
                nmsSpider.getAttributeInstance(GenericAttributes.FOLLOW_RANGE).setValue(128.0);

                spider.setRemoveWhenFarAway(false);
                spider.setCanPickupItems(false);

                spiders.put(spider, gp);

                if (variant == VARIANT_SUPER_SPIDERS)
                    nmsSpider.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0.7);
            }
        }
    }

    @Override
    public void spawnGamer(Gamer gamer) {
        super.spawnGamer(gamer);

        gamer.player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SPEED,
                        Integer.MAX_VALUE,
                        0,
                        true
                )
        );
    }

    @Override
    protected void innerOnStart() {
    }

    @Override
    protected void innerOnEnd() {
        for (Spider spider : spiders.keySet()) {
            if (spider != null && spider.isValid())
                spider.remove();
        }
        spiders.clear();
    }


    @Override
    protected void innerOnTick() {
        if (!hasStarted()) {
            for (Gamer gp : getPlayingGamers()) {
                EntityUtil.setPosOnly(gp.player, getGamerSpawn(gp).pos);
            }
        }

        if (getTicksElapsed() == SPIDER_RELEASE_DELAY_TICKS) {
            for (Gamer gp : getPlayingGamers()) {
                GameSpawn spawn = getGamerSpawn(gp);
                Vec3i grateStartBlock = spawn.getBlockPos().alongBlockFace(spawn.getDir(), -4);
                var blockRange = BlockRange.fromCorners(grateStartBlock, grateStartBlock).expand(new Vec3i(4, 4, 4));
                for (Vec3i blockPos : blockRange) {
                    Block block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
                    if (block.getType() == Material.IRON_FENCE)
                        block.setType(Material.AIR);
                }

                Display.sendPopupText(gp.player, "&c&oThe spiders have been released!");
            }
        }

        if (getTicksElapsed() >= SPIDER_RELEASE_DELAY_TICKS) {
            for (var entry : spiders.entrySet()) {
                Spider spider = entry.getKey();
                Gamer target = entry.getValue();
                if (spider == null || !spider.isValid() || target == null || target.player == null)
                    continue;
                if (target.player.getGameMode() == GameMode.SPECTATOR)
                    continue;

                var nmsSpider = ((CraftSpider)spider).getHandle();
                if (nmsSpider != null && target.player != null) {
                    nmsSpider.setGoalTarget(((CraftPlayer) target.player).getHandle());
                }
            }
        }

        for (Gamer gp : getPlayingGamers()) {
            Location loc = gp.player.getLocation();
            var winDelta2d = WIN_POS.clone().subtract(loc.toVector()).setY(0);
            if (winDelta2d.length() <= WIN_RANGE) {
                Display.sendMsg(world, "{} finished the maze!", gp.player);
                gp.stopPlaying(true);
                gp.player.playSound(gp.player.getLocation(), org.bukkit.Sound.LEVEL_UP, 0.8f, 1.5f);
            }
        }
    }
}