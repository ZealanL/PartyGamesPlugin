package zealan.pgp.game.games;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPig;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import zealan.pgp.bossbar.BossBarContent;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BlockRange;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;
import zealan.pgp.util.PlayerUtil;

import java.util.HashMap;

public class GamePigJousting extends Game {
    private static final BlockRange FENCE_BLOCK_RANGE = BlockRange.fromCorners(
            -2478, 30, 746,
            -2435, 35, 703
    );
    private static final double MOVE_SPEED = 7.0;

    private final HashMap<Gamer, Pig> pigMap = new HashMap<>();
    private int deathmatchTimer = 60;

    public GamePigJousting(InitParams params) {
        super(params);
    }

    @Override
    public BossBarContent updateBossBar(Gamer gamer) {
        if (deathmatchTimer > 0) {
            String name = "&l&cDeathmatch In: &e" + deathmatchTimer + "s";
            float progress = deathmatchTimer / (float)60;
            return new BossBarContent(name, progress);
        } else {
            String name = "&l&cDEATHMATCH";
            return new BossBarContent(name, 0f);
        }
    }

    @Override
    public void spawnGamer(Gamer gamer) {
        super.spawnGamer(gamer);

        var chunk = gamer.player.getLocation().getChunk();
        if (!chunk.isLoaded())
            chunk.load();
        Pig pig = (Pig) world.spawnEntity(gamer.player.getLocation(), EntityType.PIG);
        pig.setRemoveWhenFarAway(false);
        pig.setSaddle(true);
        pigMap.put(gamer, pig);

        gamer.player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));

        gamer.player.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
        gamer.player.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        gamer.player.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        gamer.player.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
    }

    @Override
    protected void innerOnStart() {
        for (Vec3i blockPos : FENCE_BLOCK_RANGE) {
            var block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
            if (block.getType().equals(Material.FENCE)) {
                block.setType(Material.AIR);
            }
        }

        for (var gamer : getPlayingGamers()) {
            Pig pig = pigMap.get(gamer);

            // Bat between pig and player (for SOME REASON)
            Bat bat = (Bat)world.spawnEntity(pig.getLocation(), EntityType.BAT);
            EntityUtil.setSilent(bat, true);
            EntityUtil.makeInvisible(bat);
            pig.setPassenger(bat);

            PlayerUtil.syncMount(gamer.player, bat);
        }
    }

    @Override
    protected void innerOnTick() {
        if (!hasStarted())
            return;

        if (getTicksElapsed() % 20 == 0) {
            if (deathmatchTimer > 0) {
                deathmatchTimer--;
            }

            if (deathmatchTimer <= 0) {
                for (var gamer : getPlayingGamers())
                    gamer.player.damage(1.0);
            }
        }

        for (var gamer : getPlayingGamers()) {
            Pig pig = pigMap.get(gamer);

            float yaw = EntityUtil.getYaw(gamer.player);
            double yawRad = Math.toRadians(yaw);
            Vector forwardDir = new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad));
            Vector moveDelta = forwardDir.multiply(MOVE_SPEED / 20.0);

            EntityUtil.setYaw(pig, yaw);
            EntityUtil.setHeadYaw(pig, yaw);
            var nmsPig = ((CraftPig)pig).getHandle();
            nmsPig.move(moveDelta.getX(), 0, moveDelta.getZ());
        }

        if (getPlayingGamers().size() <= 1 && getGamers().size() > 1) {
            this.end(true);
        }
    }

    @Override
    protected void innerOnEnd() {

    }

    @Override
    public boolean canAttackEntity(Player player, Entity entity, EntityDamageByEntityEvent event) {
        return hasStarted() && (entity instanceof Player);
    }

    @Override
    public boolean canUseItem(Player player, ItemStack item) {
        return true;
    }

    @Override
    public void onPlayerDeath(Player player) {
        super.onPlayerDeath(player);
        var gamer = getGamer(player);
        if (gamer != null) {
            if (pigMap.containsKey(gamer)) {
                Pig pig = pigMap.get(gamer);
                world.playSound(pig.getLocation(), Sound.PIG_DEATH, 1.0f, 1.0f);
                pig.remove();
            }
        }
    }
}
