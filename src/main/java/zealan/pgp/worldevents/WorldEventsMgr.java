package zealan.pgp.worldevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.InventoryHolder;
import zealan.pgp.AutoListener;
import zealan.pgp.util.EntityUtil;
import zealan.pgp.util.PlayerUtil;

import java.util.HashMap;
import java.util.HashSet;

import static zealan.pgp.Globals.PLOG;
import static zealan.pgp.Globals.PLUGIN;

public class WorldEventsMgr extends AutoListener {
    private final HashMap<World, WorldEvents> map = new HashMap<>();
    private final WorldEvents defaultPerms = new WorldEvents() {};
    private final HashSet<Player> needsInvUpdateSet = new HashSet<>();

    public WorldEventsMgr() {
        PacketEvents.getAPI().getEventManager().registerListener(new EventsPacketListener());
    }

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

    private class EventsPacketListener extends PacketListenerAbstract {
        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            Player player = (Player) event.getPlayer();
            if (player == null) return;

            var vehicle = player.getVehicle();
            if (vehicle != null) {
                if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
                    var packet = new WrapperPlayClientSteerVehicle(event);
                    if (packet.isUnmount()) {
                        if (!canDismount(player, vehicle))
                            event.setCancelled(true);
                    }
                } else if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
                    var packet = new WrapperPlayClientEntityAction(event);
                    if (packet.getAction() == WrapperPlayClientEntityAction.Action.START_SNEAKING) {
                        if (!canDismount(player, vehicle)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }

            if (event.getPacketType() == PacketType.Play.Client.PLAYER_ABILITIES) {
                var packet = new WrapperPlayClientPlayerAbilities(event);
                if (packet.isFlying()) {
                    if (!canStartFlying(player)) {
                        event.setCancelled(true);
                        Bukkit.getScheduler().runTask(PLUGIN, () -> {
                            player.setFlying(false);
                            PlayerUtil.sendAbilities(player);
                        });
                    }
                }
            }
        }
    }

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

        if (event instanceof EntityEvent) {
            World world = ((EntityEvent)event).getEntity().getWorld();
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
            }
        }

        if (event.hasBlock() && event.getClickedBlock() != null && event.useInteractedBlock() != Event.Result.DENY) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (!worldEvents.canInteractBlock(event.getPlayer(), event.getClickedBlock()))
                    event.setCancelled(true);
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
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                if (!worldEvents.canAttackEntity(player, event.getEntity(), event))
                    event.setCancelled(true);
            }
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

    boolean canDismount(Player player, Entity vehicle) {
        WorldEvents worldEvents = getWorldPerms(player.getWorld());
        if (worldEvents == null) return true;
        return worldEvents.canDismount(player, vehicle);
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

    // Prevent eggs from spawning chickens
    @EventHandler
    void handle(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.EGG)
            event.setCancelled(true);
    }

    @EventHandler
    void handle(EntityDamageEvent event) {
        // Disable suffocation damage
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION)
            event.setCancelled(true);

        // Disable fall damage
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL)
            event.setCancelled(true);

        // Disable fire damage
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE)
            event.setCancelled(true);
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK)
            event.setCancelled(true);

        WorldEvents worldEvents = getWorldPerms(event.getEntity().getWorld());
        if (worldEvents == null) return;
        worldEvents.onEntityDamage(event.getEntity(), event.getCause());
    }

    @EventHandler
    void handle(EntityCombustEvent event) {
        event.setCancelled(true);
    }

    // Never allow items to be damaged
    @EventHandler
    void handle(PlayerItemDamageEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    void handle(ProjectileHitEvent event) {
        WorldEvents worldEvents = getWorldPerms(event.getEntity().getWorld());
        if (worldEvents == null) return;
        worldEvents.onProjectileHit(event.getEntity(), event);
    }

    // Never allow a vehicle to be damaged
    @EventHandler
    void handle(VehicleDamageEvent event) {
        event.setCancelled(true);
    }

    boolean canStartFlying(Player player) {
        WorldEvents worldEvents = getWorldPerms(player.getWorld());
        if (worldEvents == null) return true;
        return worldEvents.canStartFlying(player);
    }

    // //////////

    @Override
    public void onTick() {
        for (Player player : needsInvUpdateSet)
            player.updateInventory();
        needsInvUpdateSet.clear();
    }
}
