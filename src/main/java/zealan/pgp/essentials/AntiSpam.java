package zealan.pgp.essentials;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import zealan.pgp.AutoListener;
import zealan.pgp.api.display.Display;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiSpam extends AutoListener {
    static class SpamAccum {
        static final int SPAM_INCREASE_TICKS_CHAT = 80;
        static final int SPAM_INCREASE_TICKS_COMMAND = 60;
        static final int SPAM_THRESHOLD_TICKS = 120;

        private int chat = 0;
        private int commands = 0;

        int get(boolean isChat) {
            return isChat ? chat : commands;
        }

        void set(boolean isChat, int newVal) {
            if (isChat) {
                chat = newVal;
            } else {
                commands = newVal;
            }
        }

        void inc(boolean isChat) {
            if (isChat) {
                chat += SPAM_INCREASE_TICKS_CHAT;
            } else {
                commands += SPAM_INCREASE_TICKS_COMMAND;
            }
        }

        void decayTick() {
            chat = Math.max(0, chat - 1);
            commands = Math.max(0, commands - 1);
        }

        boolean isZero() {
            return chat <= 0 && commands <= 0;
        }
    }

    final HashMap<UUID, SpamAccum> spamAccums = new HashMap<>();

    boolean updateSpamCheck(Player player, boolean isChat) {
        if (player.isOp())
            return false;

        SpamAccum cur = spamAccums.getOrDefault(player.getUniqueId(), new SpamAccum());
        if (cur.get(isChat) >= SpamAccum.SPAM_THRESHOLD_TICKS)
            return true;

        cur.inc(isChat);
        spamAccums.put(player.getUniqueId(), cur);
        return false;
    }

    @Override
    public void onTick() {
        for (Map.Entry<UUID, SpamAccum> entry : spamAccums.entrySet())
            entry.getValue().decayTick();

        spamAccums.entrySet().removeIf(
                entry -> entry.getValue().isZero()
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (updateSpamCheck(event.getPlayer(), true)) {
            Display.sendMsg(event.getPlayer(), "&cDon't spam chat, it's rude!");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (updateSpamCheck(event.getPlayer(), false)) {
            Display.sendMsg(event.getPlayer(), "&cDon't spam commands, it's rude!");
            event.setCancelled(true);
        }
    }

    // Fix stupid spigot spam kick
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        String reason = event.getReason();

        if (reason != null && (reason.contains("disconnect.spam") || reason.toLowerCase().contains("spamming"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        spamAccums.remove(event.getPlayer().getUniqueId());
    }
}

