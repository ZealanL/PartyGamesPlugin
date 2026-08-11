package zealan.pgp.essentials;

import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import zealan.pgp.AutoListener;
import zealan.pgp.api.command.CommandNode;
import zealan.pgp.api.command.CommandResult;
import zealan.pgp.api.display.Display;

import static zealan.pgp.Globals.COMMAND_SYS;

public class Essentials extends AutoListener {

    private final AntiSpam antiSpam = new AntiSpam();
    private final CommandBlocker commandBlocker = new CommandBlocker();

    public Essentials() {
        COMMAND_SYS.register(
                CommandNode.make(
                        "ping",
                        "See your ping",
                        ctx -> {
                            int pingMs = ((CraftPlayer)ctx.sender).getHandle().ping;
                            return CommandResult.ok("&oYour ping is &6" + pingMs + "ms");
                        }
                )
        );
    }

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        event.setJoinMessage(Display.format("{} &ajoined!", event.getPlayer()));
        Display.sendPopupText(event.getPlayer(), "&aWelcome!");
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        event.setQuitMessage(Display.format("{} &7left.", event.getPlayer()));
    }
}
