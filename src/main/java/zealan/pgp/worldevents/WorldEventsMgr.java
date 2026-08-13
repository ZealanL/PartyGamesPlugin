package zealan.pgp.worldevents;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.spigotmc.event.entity.EntityDismountEvent;
import zealan.pgp.AutoListener;

import java.util.HashMap;
import java.util.HashSet;

public class WorldEventsMgr extends AutoListener {
    private final HashMap<World, WorldEvents> map = new HashMap<>();
    private final WorldEvents defaultPerms = new WorldEvents() {};
    private final HashSet<Player> needsInvUpdateSet = new HashSet<>();

    public void register(World world, WorldEvents worldEvents) {
        synchronized (map) {
            this.map.put(world, worldEvents);
        }
    }

    public void unregister(World world, WorldEvents worldEvents) {
        synchronized (map) {
            if (!this.map.containsKey(world))
                throw new IllegalStateException("No registration made for world");
            if (!this.map.get(world).equals(worldEvents))
                throw new IllegalStateException("World perms registrar is not equal");
            this.map.remove(world);
        }
    }

    public WorldEvents getWorldPerms(World world) {
        return this.map.getOrDefault(world, defaultPerms);
    }

    // /////////////////

    private Player getEventPlayer(Event event) {
        if (event instanceof PlayerEvent)
            return ((PlayerEvent) event).getPlayer();

        if (event instanceof BlockBreakEvent)
            return ((BlockBreakEvent) event).getPlayer();

        if (event instanceof BlockPlaceEvent)
            return ((BlockPlaceEvent) event).getPlayer();

        if (event instanceof EntityDamageByEntityEvent) {
            Entity attacker = ((EntityDamageByEntityEvent)event).getDamager();
            if (attacker instanceof Player) {
                return (Player) attacker;
            }
        }

        if (event instanceof EntityEvent) {
            Entity entity = ((EntityEvent)event).getEntity();
            if (entity instanceof Player) {
                return (Player) entity;
            }
        }

        if (event instanceof InventoryEvent) {
            InventoryHolder holder = ((InventoryEvent)event).getInventory().getHolder();
            if (holder instanceof Player) {
                return (Player) holder;
            }
        }

        return null;
    }

    private WorldEvents getWorldEventsFromEvent(Event event) {
        Player player = getEventPlayer(event);
        if (player != null) {
            if (player.getGameMode() == GameMode.CREATIVE && player.isOp()) {
                // Bypass event check
                return null;
            }

            World world = player.getWorld();
            return getWorldPerms(world);
        }

        return null;
    }

    // ///////////////

    @EventHandler
    void handle(BlockBreakEvent event) {
        WorldEvents worldEvents = getWorldEventsFromEvent(event);
        if (worldEvents == null) return;

        if (!worldEvents.canBreakBlock(event.getPlayer(), event.getBlock()))
            event.setCancelled(true);
    }

    // TODO: Unused, replaced with PlayerInteractEvent
    /*
    @EventHandler
    void handle(BlockPlaceEvent event) {
        WorldPerms worldPerms = getEventWorldPerms(event);
        if (worldPerms == null) return;

        if (!worldPerms.canPlaceBlock(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            needsInvUpdate.add(event.getPlayer());
        }
    }
    */

    @EventHandler
    void handle(PlayerInteractEvent event) {
        WorldEvents worldEvents = getWorldEventsFromEvent(event);
        if (worldEvents == null) return;

        if (event.hasItem() && event.useItemInHand() != Event.Result.DENY) {
            if (!worldEvents.canUseItem(event.getPlayer(), event.getItem())) {
                event.setCancelled(true);
                needsInvUpdateSet.add(event.getPlayer());
            } else {
                return; // Don't reach block event detection
            }
        }

        if (event.hasBlock() && event.getClickedBlock() != null && event.useInteractedBlock() != Event.Result.DENY) {
            if (!worldEvents.canInteractBlock(event.getPlayer(), event.getClickedBlock())) {
                if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
                    event.setCancelled(true);
                } else {
                    // If it's block breaking, it will be handled in the block break event
                }
            }
        }
    }

    @EventHandler
    void handle(PlayerInteractEntityEvent event) {
        WorldEvents worldEvents = getWorldEventsFromEvent(event);
        if (worldEvents == null) return;
        if (!worldEvents.canInteractEntity(event.getPlayer(), event.getRightClicked()))
            event.setCancelled(true);
    }

    @EventHandler
    void handle(EntityDamageByEntityEvent event) {
        WorldEvents worldEvents = getWorldEventsFromEvent(event);
        if (worldEvents == null) return;
        if (event.getDamager() instanceof Player) {
            if (!worldEvents.canAttackEntity((Player)event.getDamager(), event.getEntity(), event))
                event.setCancelled(true);
        }
    }

    @EventHandler
    void handle(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        WorldEvents worldEvents = getWorldEventsFromEvent(event);
        if (worldEvents == null) return;
        if (!worldEvents.canInteractInv((Player)event.getWhoClicked(), event.getInventory()))
            event.setCancelled(true);
    }

    @EventHandler
    void handle(EntityDismountEvent event) {
        WorldEvents worldEvents = getWorldEventsFromEvent(event);
        if (worldEvents == null) return;
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!worldEvents.canDismount(player, event.getDismounted()))
                event.getDismounted().setPassenger(player);
        }
    }

    @EventHandler
    void handle(PlayerDropItemEvent event) {
        WorldEvents worldEvents = getWorldEventsFromEvent(event);
        if (worldEvents == null) return;
        if (!worldEvents.canDropItem(event.getPlayer(), event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            needsInvUpdateSet.add(event.getPlayer());
        }
    }

    @EventHandler
    void handle(PlayerDeathEvent event) {
        var player = event.getEntity();

        WorldEvents worldEvents = getWorldEventsFromEvent(event);
        if (worldEvents == null) return;
        worldEvents.onPlayerDeath(player);

        // Never actually allow a player death
        player.setHealth(player.getMaxHealth());
    }

    // //////////


    @Override
    public void onTick() {
        for (Player player : needsInvUpdateSet)
            player.updateInventory();
        needsInvUpdateSet.clear();
    }
}
