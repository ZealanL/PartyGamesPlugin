package zealan.pgp.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import zealan.pgp.api.display.Display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static zealan.pgp.Globals.MENU_MGR;

public class MenuInv implements InventoryHolder {
    public enum Size {
        SMALL(3, 3, InventoryType.DROPPER),
        ROW(9, 1, InventoryType.CHEST),
        LARGE(9, 5, InventoryType.CHEST);

        public final int x;
        public final int y;
        public final int numSlots;
        public final InventoryType type;

        Size(int x, int y, InventoryType type) {
            this.x = x;
            this.y = y;
            this.numSlots = x * y;
            this.type = type;
        }
    }

    public enum Ordering {
        LEFT_TO_RIGHT,
        CENTERED
    }

    public record ItemEntry(ItemStack itemStack, String itemLabel, String description, String command) {
    }

    // //////////////////////

    public final Player player;
    public final String name;
    public final Size size;
    public final Ordering ordering;
    private boolean open = false;
    private final ArrayList<ItemEntry> itemEntries = new ArrayList<>();
    private HashMap<Integer, ItemEntry> slotMap = null;
    private Inventory inventory;

    public MenuInv(Player player, String name, Size size, Ordering ordering) {
        this.player = player;
        this.name = name;
        this.size = size;
        this.ordering = ordering;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    void addItemEntry(ItemEntry itemEntry) {
        if (itemEntries.size() < this.size.numSlots) {
            itemEntries.add(itemEntry);
        } else {
            throw new RuntimeException("Tried to add too many items");
        }
    }

    void updateSlotMap() {
        this.slotMap = new HashMap<>();
        int numEntries = this.itemEntries.size();
        switch (this.ordering) {
            case LEFT_TO_RIGHT -> {
                for (int i = 0; i < this.itemEntries.size(); i++)
                    slotMap.put(i, this.itemEntries.get(i));
            }
            case CENTERED -> {
                int rectSizeX = 1;
                int rectSizeY = 1;
                while ((rectSizeX * rectSizeY) < numEntries) {
                    int remainingWidth = this.size.x - rectSizeX;
                    int remainingHeight = this.size.y - rectSizeY;
                    if (remainingWidth >= remainingHeight && remainingWidth >= 2) {
                        rectSizeX += 2;
                    } else {
                        rectSizeY += 1;
                    }
                }
                int rectStartX = (this.size.x / 2) - (rectSizeX / 2);
                int rectStartY = (this.size.y / 2) - (rectSizeY / 2);

                for (int dy = 0; dy < rectSizeY; dy++) {
                    for (int dx = 0; dx < rectSizeX; dx++) {
                        int rectIdx = dx + dy * rectSizeX;
                        if (rectIdx >= numEntries)
                            break;

                        int slotIdx = (dx + rectStartX) + ((this.size.x) * (dy + rectStartY));
                        if (slotIdx >= this.size.numSlots)
                            throw new RuntimeException("Slot index exceeded inventory rectangle");

                        slotMap.put(slotIdx, this.itemEntries.get(rectIdx));
                    }
                }
            }
        }
        assert this.slotMap.size() == this.itemEntries.size();
    }

    void show(Player player) {
        if (open)
            throw new RuntimeException("Already open");

        updateSlotMap();

        if (this.size.type == InventoryType.CHEST) {
            this.inventory = Bukkit.createInventory(this, this.size.numSlots, this.name);
        } else {
            this.inventory = Bukkit.createInventory(this, this.size.type, this.name);
        }

        for (var pair : this.slotMap.entrySet()) {
            var slotIdx = pair.getKey();
            var itemEntry = pair.getValue();

            if (itemEntry == null)
                continue;

            ItemStack itemStack = MENU_MGR.makeCommandItem(
                    itemEntry.itemStack().clone(), itemEntry.itemLabel(), itemEntry.command()
            );
            ItemMeta meta = itemStack.getItemMeta();

            if (meta != null) {
                if (itemEntry.itemLabel() != null) {
                    meta.setDisplayName(Display.format(itemEntry.itemLabel()));
                }
                if (itemEntry.description() != null) {
                    meta.setLore(List.of(Display.format(itemEntry.description())));
                }

                meta.addItemFlags(ItemFlag.values());
                itemStack.setItemMeta(meta);
            }

            this.inventory.setItem(slotIdx, itemStack);
        }

        ItemStack carried = player.getItemOnCursor();
        if (carried.getType() != Material.AIR) {
            player.getWorld().dropItemNaturally(player.getLocation(), carried);
            player.setItemOnCursor(null);
        }

        player.openInventory(this.inventory);
        this.open = true;
    }

    boolean isOpen() {
        return this.open;
    }

    public void stopOpen(Player user) {
        this.open = false;
        user.closeInventory();
    }
}
