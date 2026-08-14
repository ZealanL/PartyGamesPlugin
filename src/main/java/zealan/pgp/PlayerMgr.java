package zealan.pgp;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import zealan.pgp.api.command.CommandArg;
import zealan.pgp.api.command.CommandNode;
import zealan.pgp.api.command.CommandResult;
import zealan.pgp.api.display.Display;
import zealan.pgp.math.MoveInput3;
import zealan.pgp.util.PlayerUtil;
import zealan.pgp.util.WorldUtil;

import java.util.HashSet;
import java.util.UUID;
import java.util.WeakHashMap;

import static zealan.pgp.Globals.*;

public class PlayerMgr extends AutoListener {
    private final HashSet<UUID> fakeSpectatorUUIDs = new HashSet<>();
    private final WeakHashMap<Player, MoveInput3> moveInputMap = new WeakHashMap<>();

    private class PlayerPacketListener extends PacketListenerAbstract {
        public PlayerPacketListener() {
            super(PacketListenerPriority.HIGHEST);
        }

        @Override
        public void onPacketSend(PacketSendEvent event) {
            if (event.getPacketType() == PacketType.Play.Server.SPAWN_PLAYER) {
                var spawnPacket = new WrapperPlayServerSpawnPlayer(event);
                if (fakeSpectatorUUIDs.contains(spawnPacket.getUUID())) {
                    event.setCancelled(true);
                }
            }
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
                Player player = (Player) event.getPlayer();
                var packet = new WrapperPlayClientSteerVehicle(event);
                MoveInput3 movementInput = new MoveInput3(0, 0, 0);

                float forwardInput = packet.getForward();
                float sidewaysInput = packet.getSideways();
                boolean jumping = packet.isJump();
                boolean sneaking = packet.isUnmount();

                movementInput.forward = Math.clamp(forwardInput, -1, 1);
                movementInput.sideways = Math.clamp(sidewaysInput, -1, 1);
                if (jumping)
                    movementInput.up += 1;
                if (sneaking)
                    movementInput.up -= 1;

                moveInputMap.put(player, movementInput);
            }
        }
    }

    public PlayerMgr() {
        COMMAND_SYS.register(CommandNode.make("lobby", "Return to the main lobby", ctx -> {
            PLAYER_MGR.sendToLobby(ctx.sender, true);
            return CommandResult.ok();
        }).withAlias("spawn"));

        COMMAND_SYS.register(CommandNode.make("spec", "Spectate, optionally spectate a specific player", ctx -> {
            Player target = ctx.getArg("target_player");

            if (setFakeSpectator(ctx.sender, !isFakeSpectator(ctx.sender))) {
                if (target != null) {
                    ctx.sender.teleport(target);
                    return CommandResult.ok("&9You are now spectating: {}", target);
                } else {
                    return CommandResult.ok("&9You are now spectating.");
                }
            } else {
                PLAYER_MGR.sendToLobby(ctx.sender, true);
                Display.sendPopupText(ctx.sender, "");
                return CommandResult.ok("&9You are no longer spectating.");
            }
        }, new CommandArg.PlayerArg("target_player").makeOptional()).withAlias("spectate"));

        PacketEvents.getAPI().getEventManager().registerListener(new PlayerPacketListener());
    }

    public void givePlayerLobbyInv(Player player) {
        player.getInventory().clear();

        var specialCompass = MENU_MGR.makeCommandItem(Material.COMPASS, "&dGames", "play");
        player.getInventory().setItem(0, specialCompass);

        var specialBook = MENU_MGR.makeCommandItem(Material.BOOK, "&6View stats", "stats {name}");
        player.getInventory().setItem(1, specialBook);

        var specialPaper = MENU_MGR.makeCommandItem(Material.PAPER, "&bReplay last game", "playagain");
        player.getInventory().setItem(2, specialPaper);
    }

    public void sendToLobby(Player player, boolean sendMessage) {
        player.teleport(WorldUtil.getMainWorld().getSpawnLocation());
        player.setGameMode(GameMode.ADVENTURE);
        PlayerUtil.cleanPlayer(player);
        setFakeSpectator(player, false);

        givePlayerLobbyInv(player);

        if (sendMessage) {
            Display.sendMsg(player, "&7&o(You were sent to the lobby)");
            WorldUtil.getMainWorld().playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 0.4f, 1.2f);
        }
    }

    @Override
    public void onTick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE) continue;

            player.setSaturation(5);
            player.setFoodLevel(20);

            if (player.getWorld() == WorldUtil.getMainWorld()) {
                Location spawnPos = player.getWorld().getSpawnLocation();
                if (player.getLocation().distance(spawnPos) > 60) player.teleport(spawnPos);
            }

            if (isFakeSpectator(player)) {
                Display.sendPopupText(player, "&e&l( SPECTATING )");
                player.setFlying(true);
            }
        }
    }

    // Remove fall damage
    @EventHandler
    void handle(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) event.setCancelled(true);

        // Spectators can't be hurt
        if (event.getEntity() instanceof Player player)
            if (fakeSpectatorUUIDs.contains(player.getUniqueId())) event.setCancelled(true);
    }

    // Send joining players to the lobby
    @EventHandler
    void handle(PlayerJoinEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE && event.getPlayer().isOp()) return;

        sendToLobby(event.getPlayer(), false);
    }

    @EventHandler
    void handle(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player)
            if (fakeSpectatorUUIDs.contains(player.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    void handle(PlayerInteractEvent event) {
        if (fakeSpectatorUUIDs.contains(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    void handle(PlayerInteractEntityEvent event) {
        if (fakeSpectatorUUIDs.contains(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    void handle(PlayerQuitEvent event) {
        fakeSpectatorUUIDs.remove(event.getPlayer().getUniqueId());
    }

    private void reloadPlayerForOthers(Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;

            online.hidePlayer(player);
            Bukkit.getScheduler().runTaskLater(PLUGIN, () -> {
                if (player.isOnline() && online.isOnline()) online.showPlayer(player);
            }, 2L);
        }
    }

    private void startFakeSpectator(Player player) {
        PlayerUtil.cleanPlayer(player);

        player.setGameMode(GameMode.ADVENTURE);
        player.spigot().setCollidesWithEntities(false);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 1));
        reloadPlayerForOthers(player);

        // NOTE: For some reason this must be delayed?
        Bukkit.getScheduler().runTaskLater(PLUGIN, () -> givePlayerLobbyInv(player), 1);
    }

    private void endFakeSpectator(Player player) {
        PlayerUtil.cleanPlayer(player);
        reloadPlayerForOthers(player);
    }

    public boolean isFakeSpectator(Player player) {
        return fakeSpectatorUUIDs.contains(player.getUniqueId());
    }

    public boolean setFakeSpectator(Player player, boolean fakeSpectator) {
        if (isFakeSpectator(player) != fakeSpectator) {
            if (fakeSpectator) {
                fakeSpectatorUUIDs.add(player.getUniqueId());
                startFakeSpectator(player);
            } else {
                fakeSpectatorUUIDs.remove(player.getUniqueId());
                endFakeSpectator(player);
            }
        }

        return fakeSpectator;
    }

    public MoveInput3 getMoveInput(Player player) {
        var result = moveInputMap.get(player);
        if (result == null)
            return new MoveInput3();
        return result;
    }
}
