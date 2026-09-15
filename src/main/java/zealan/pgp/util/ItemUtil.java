package zealan.pgp.util;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;

public class ItemUtil {
    public static ItemStack getSpawnEgg(EntityType entityType) {
        return new ItemStack(Material.MONSTER_EGG, 1, entityType.getTypeId());
    }

    public static ItemStack getColoredWool(DyeColor color) {
        return new Wool(color).toItemStack();
    }

    public static String getMaterialNameLower(Material material) {
        return material.name().replace("_", " ").toLowerCase();
    }
}
