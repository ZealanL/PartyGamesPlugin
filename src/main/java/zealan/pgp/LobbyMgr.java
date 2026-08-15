package zealan.pgp;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import zealan.pgp.api.display.Display;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.PlayerUtil;
import zealan.pgp.util.WorldUtil;

import java.util.List;

import static zealan.pgp.Globals.*;
import static zealan.pgp.Globals.MENU_MGR;

public class LobbyMgr extends AutoListener {
    public void givePlayerLobbyInv(Player player) {
        player.getInventory().clear();

        var specialCompass = MENU_MGR.makeCommandItem(Material.COMPASS, "&dGames", "play");
        player.getInventory().setItem(0, specialCompass);

        var specialBook = MENU_MGR.makeCommandItem(Material.BOOK, "&6View your stats", "stats {name}");
        player.getInventory().setItem(1, specialBook);

        var specialPaper = MENU_MGR.makeCommandItem(Material.PAPER, "&bReplay last game", "playagain");
        player.getInventory().setItem(2, specialPaper);
    }

    public void sendToLobby(Player player, boolean sendMessage) {
        player.teleport(WorldUtil.getMainWorld().getSpawnLocation());
        player.setGameMode(GameMode.ADVENTURE);
        PlayerUtil.cleanPlayer(player);
        PLAYER_MGR.setFakeSpectator(player, false);

        givePlayerLobbyInv(player);

        if (sendMessage) {
            Display.sendMsg(player, "&7&o(You were sent to the lobby)");
            WorldUtil.getMainWorld().playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 0.4f, 1.2f);
        }
    }

    private void updateArmorStandShowcases() {
        {
            final Vec3i MAIN_INFO_POS = new Vec3i(23, 116, 206);
            ARMOR_STAND_MGR.setAtPos(MAIN_INFO_POS, List.of(
                    "&f&lWelcome!",
                    "&eUse the compass to select a game",
                    "&7You can also do &f/help&7 for a list of commands"
            ));
        }

        {
            final int LB_SHOWCASE_CYCLE_TICKS = 20 * 5;
            final Vec3i LB_SHOWCASE_POS = new Vec3i(23, 116, 199);
            if (getListenerTickCount() % LB_SHOWCASE_CYCLE_TICKS == 0) {
                var leaderboards = LEADERBOARD_MGR.getAllLeaderboards();
                if (leaderboards.isEmpty())
                    return;
                int showcaseIdx = (getListenerTickCount() / LB_SHOWCASE_CYCLE_TICKS) % leaderboards.size();
                var showcaseBoard = leaderboards.get(showcaseIdx);

                var lines = showcaseBoard.toMsgLines();
                lines.add(0, Display.format("&7Use &f/lb &7to view the leaderboard:"));
                ARMOR_STAND_MGR.setAtPos(LB_SHOWCASE_POS, lines);
            }
        }
    }

    @Override
    public void onTick() {
        updateArmorStandShowcases();
    }
}
