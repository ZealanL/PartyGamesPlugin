package zealan.pgp.game.games;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.Vec3i;

public class GameHighGround extends Game {
    static final int TOP_GROUND_Y = 41;
    static final int FALL_RESET_Y = 25;

    public GameHighGround(InitParams params) {
        super(params);
    }

    @Override
    protected void innerOnStart() {
        for (Gamer gamer : getPlayingGamers()) {
            spawnGamer(gamer);

            var slapFish = new ItemStack(Material.RAW_FISH);
            slapFish.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
            var meta = slapFish.getItemMeta();
            meta.setDisplayName("Fishy");
            slapFish.setItemMeta(meta);

            gamer.player.getInventory().setItem(0, slapFish);
        }
    }

    @Override
    protected void innerOnTick() {
        if (getTicksElapsed() % 20 == 0) {
            for (Gamer gamer : getPlayingGamers()) {
                Vec3i blockPos = Vec3i.from(gamer.player.getLocation().toVector());
                blockPos.y = TOP_GROUND_Y;

                if (world.getBlockAt(blockPos.x, blockPos.y, blockPos.z).getType() == Material.WOOL) {
                    gamer.points++;
                }
            }
        }

        for (Gamer gamer : getPlayingGamers()) {
            if (gamer.player.getLocation().getY() < FALL_RESET_Y) {
                spawnGamer(gamer);
            }
        }
    }

    @Override
    protected void innerOnEnd() {

    }

    @Override
    public boolean canAttackEntity(Player player, Entity entity, EntityDamageByEntityEvent event) {
        event.setDamage(1e-5);
        return hasStarted();
    }
}
