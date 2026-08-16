package zealan.pgp;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerListPingEvent;
import zealan.pgp.api.display.Display;

import java.util.Random;

import static zealan.pgp.Globals.LEADERBOARD_MGR;
import static zealan.pgp.Globals.PLAYER_STATS_MGR;

public class MOTDMgr extends AutoListener {
    private static final Random random = new Random();
    @EventHandler
    void handle(ServerListPingEvent event) {
        String firstLine = "&a&lPartyGames&a.cc &e&lPractice Server &7- &d[1.8-26.2]";
        String secondLine = "&915+ Games Implemented";
        var leaderboards = LEADERBOARD_MGR.getAllLeaderboards();
        if (!leaderboards.isEmpty()) {
            var leaderboard = leaderboards.get(random.nextInt(leaderboards.size()));
            var topEntry = leaderboard.get(0);
            var stats = PLAYER_STATS_MGR.getPlayerStatsByUUID(topEntry.playerUUID());
            if (stats != null) {
                secondLine = (
                        "&7#&e1&r in &6" + leaderboard.gameConfig.properName + "&f: " + stats.playerName
                        + " &7with " + topEntry.gamePB().toMsgPart()
                );
            }
        }

        event.setMotd(
                Display.concatLines(firstLine, secondLine)
        );
    }
}
