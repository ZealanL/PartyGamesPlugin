package zealan.pgp.util;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class ItemUtil {
    public static ItemStack getSpawnEgg(EntityType entityType) {
        return new ItemStack(Material.MONSTER_EGG, 1, entityType.getTypeId());
    }
}
