package zealan.pgp.menu;

import net.minecraft.server.v1_8_R3.MinecraftKey;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import zealan.pgp.AutoListener;
import zealan.pgp.api.display.Display;

import java.util.Collection;

import static zealan.pgp.Globals.COMMAND_SYS;

public class MenuMgr extends AutoListener {
    private static final String COMMAND_ITEM_KEY = "special_command_item_cmd";

    public ItemStack makeCommandItem(Material itemType, String name, String command) {
        return makeCommandItem(new ItemStack(itemType), name, command);
    }

    public ItemStack makeCommandItem(ItemStack stack, String name, String command) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null)
                meta.setDisplayName(Display.format(name));
            stack.setItemMeta(meta);
        }

        if (command != null) {
            net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
            NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();

            tag.setString(COMMAND_ITEM_KEY, command);
            nmsStack.setTag(tag);

            stack = CraftItemStack.asBukkitCopy(nmsStack);
        }

        return stack;
    }

    public void showMenuInv(Player player, String name, Collection<MenuInv.ItemEntry> itemEntries, MenuInv.Size size) {
        var inv = new MenuInv(
                player, name, size,
                size.numSlots > 9 ? MenuInv.Ordering.CENTERED : MenuInv.Ordering.LEFT_TO_RIGHT
        );
        for (var itemEntry : itemEntries)
            inv.addItemEntry(itemEntry);
        inv.show(player);
    }

    private boolean onPlayerInteractItem(Player player, ItemStack stack) {
        net.minecraft.server.v1_8_R3.ItemStack nmsSTack = CraftItemStack.asNMSCopy(stack);
        if (nmsSTack != null && nmsSTack.hasTag()) {
            NBTTagCompound tag = nmsSTack.getTag();
            if (tag.hasKey(COMMAND_ITEM_KEY)) {
                String command = tag.getString(COMMAND_ITEM_KEY);
                String commandStr = command.replace("{name}", player.getName());
                COMMAND_SYS.playerExecute(player, commandStr);
                return true;
            }
        }

        return false;
    }

    @EventHandler
    void handle(PlayerInteractEvent event) {
        if (event.getItem() != null) {
            if (onPlayerInteractItem(event.getPlayer(), event.getItem()))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void handle(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        ItemStack stack = event.getCurrentItem();
        if (stack != null && stack.getType() != Material.AIR) {
            onPlayerInteractItem(player, stack);
        }

        if (event.getClickedInventory().getHolder() instanceof MenuInv) {
            event.setCancelled(true);
        }
    }
}
