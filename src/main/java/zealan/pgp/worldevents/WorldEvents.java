package zealan.pgp.worldevents;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
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

    default boolean canAttackEntity(Player player, Entity entity, EntityDamageByEntityEvent event) {
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

    default boolean canStartFlying(Player player) {
        return true;
    }

    default void onPlayerDeath(Player player) {

    }

    default void onProjectileHit(Projectile projectile, ProjectileHitEvent event) {
    }
}
