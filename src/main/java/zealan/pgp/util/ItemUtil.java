package zealan.pgp.util;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.SpawnEgg;

public class ItemUtil {
    public static ItemStack getSpawnEgg(EntityType entityType) {
        ItemStack eggStack = new ItemStack(Material.MONSTER_EGG, 1);
        SpawnEgg eggData = new SpawnEgg();
        eggData.setSpawnedType(EntityType.CHICKEN);
        eggStack.setData(eggData);
        return eggStack;
    }
}
