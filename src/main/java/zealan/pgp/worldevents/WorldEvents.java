package zealan.pgp.worldevents;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface WorldEvents {
    default boolean canUseItem(Player player, ItemStack item) {
        return false;
    }

    default boolean canInteractBlock(Player player, Block block) {
        return false;
    }

    default boolean canInteractEntity(Player player, Entity entity) {
        return false;
    }

    default boolean canAttackEntity(Player player, Entity to, EntityDamageEvent event) {
        return false;
    }

    default boolean canInteractInv(Player player, Inventory inv) {
        return false;
    }

    default boolean canBreakBlock(Player player, Block block) {
        return false;
    }

    default boolean canDismount(Player player, Entity entity) {
        return false;
    }

    default boolean canDropItem(Player player, ItemStack item) {
        return false;
    }

    default void onPlayerDeath(Player player) {

    }
}
