package zealan.pgp.essentials;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import zealan.pgp.AutoListener;
import zealan.pgp.api.display.Display;

import java.util.Arrays;
import java.util.HashSet;

public class CommandBlocker extends AutoListener {
    private static final HashSet<String> BANNED_COMMANDS = new HashSet<>(Arrays.asList(
            "plugins",
            "pl",
            "?",
            "about",
            "about",
            "me",
            "ver",
            "version"
    ));

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().isOp())
            return;

        String withoutSlash = event.getMessage().substring(1);
        String[] parts = withoutSlash.split(" ", 2);
        String typedLabel = parts[0].toLowerCase();

        if (BANNED_COMMANDS.contains(typedLabel)) {
            Display.sendMsg(event.getPlayer(), "&cNo! Bad!");
            event.setCancelled(true);
        }
    }
}
