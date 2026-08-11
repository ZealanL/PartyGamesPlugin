package zealan.pgp;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import zealan.pgp.api.command.CommandArg;
import zealan.pgp.api.command.CommandNode;
import zealan.pgp.api.command.CommandResult;
import zealan.pgp.api.display.Display;
import zealan.pgp.util.PlayerUtil;
import zealan.pgp.util.WorldUtil;

import java.util.HashSet;
import java.util.UUID;

import static zealan.pgp.Globals.*;

public class PlayerMgr extends AutoListener {
    private final HashSet<UUID> fakeSpectatorUUIDs = new HashSet<>();

    private class PlayerPacketListener extends PacketListenerAbstract {
        public PlayerPacketListener() {
            super(PacketListenerPriority.HIGH);
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
    }

    public PlayerMgr() {
        COMMAND_SYS.register(
                CommandNode.make(
                        "lobby",
                        "Return to the main lobby",
                        ctx -> {
                            PLAYER_MGR.sendToLobby(ctx.sender, true);
                            return CommandResult.ok();
                        }
                ).withAlias("spawn")
        );

        COMMAND_SYS.register(
                CommandNode.make(
                        "spec",
                        "Spectate, optionally spectate a specific player",
                        ctx -> {
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
                        },
                        new CommandArg.PlayerArg("target_player").makeOptional()
                ).withAlias("spectate")
        );
    }

    public void sendToLobby(Player player, boolean sendMessage) {
        player.teleport(WorldUtil.getMainWorld().getSpawnLocation());
        player.setGameMode(GameMode.ADVENTURE);
        PlayerUtil.cleanPlayer(player);
        setFakeSpectator(player, false);

        if (sendMessage) {
            Display.sendMsg(player, "&7&o(You were sent to the lobby)");
            WorldUtil.getMainWorld().playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 0.4f, 1.2f);
        }
    }

    @Override
    public void onTick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.CREATIVE)
                continue;

            player.setSaturation(5);
            player.setFoodLevel(20);

            if (player.getWorld() == WorldUtil.getMainWorld()) {
                Location spawnPos = player.getWorld().getSpawnLocation();
                if (player.getLocation().distance(spawnPos) > 60)
                    player.teleport(spawnPos);
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
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL)
            event.setCancelled(true);

        // Spectators can't be hurt
        if (event.getEntity() instanceof Player player)
            if (fakeSpectatorUUIDs.contains(player.getUniqueId()))
                event.setCancelled(true);
    }

    // Send joining players to the lobby
    @EventHandler
    void handle(PlayerJoinEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE && event.getPlayer().isOp())
            return;

        sendToLobby(event.getPlayer(), false);
    }

    @EventHandler
    void handle(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player)
            if (fakeSpectatorUUIDs.contains(player.getUniqueId()))
                event.setCancelled(true);
    }

    @EventHandler
    void handle(PlayerInteractEvent event) {
        if (fakeSpectatorUUIDs.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    void handle(PlayerInteractEntityEvent event) {
        if (fakeSpectatorUUIDs.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
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
                if (player.isOnline() && online.isOnline())
                    online.showPlayer(player);
            }, 2L);
        }
    }

    private void startFakeSpectator(Player player) {
        PlayerUtil.cleanPlayer(player);
        // TODO: Remove from game

        player.setGameMode(GameMode.ADVENTURE);
        player.spigot().setCollidesWithEntities(false);
        player.setAllowFlight(true);
        player.setFlying(true);

        player.addPotionEffect(
                new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 1)
        );

        // TODO: Add special items
        //givePlayerLobbyInv(player);

        reloadPlayerForOthers(player);
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
}
