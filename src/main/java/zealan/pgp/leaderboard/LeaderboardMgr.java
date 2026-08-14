package zealan.pgp.leaderboard;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import zealan.pgp.api.command.CommandNode;
import zealan.pgp.api.command.CommandResult;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.GameCommandArg;
import zealan.pgp.game.GameConfig;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

import static zealan.pgp.Globals.*;

public class LeaderboardMgr {
    private final HashMap<GameConfig, Leaderboard> leaderboards = new HashMap<>();

    public LeaderboardMgr() {
        COMMAND_SYS.register(
                CommandNode.make(
                        "leaderboard",
                        "Get leaderboard information for a game",
                        ctx -> {
                            GameConfig gameConfig = ctx.getArg("game");
                            var lb = leaderboards.get(gameConfig);
                            if (lb == null)
                                return CommandResult.failure("This game doesn't have a leaderboard yet!");

                            return CommandResult.ok(
                                    String.join("\n", lb.toMsgLines())
                            );
                        },
                        new GameCommandArg()
                ).withAlias("lb")
        );

        fullUpdate();
    }

    public void fullUpdate() {
        for (var record : PLAYER_STATS_MGR.getAllRecords())
            for (var gameStats : record.allGameStats.values())
                updateForPlayer(record.playerUUID, gameStats.gameConfig, false);
    }

    public @Nullable Leaderboard getLeaderboard(GameConfig gameConfig) {
        return leaderboards.get(gameConfig);
    }

    public void updateForPlayer(Player sp, GameConfig gameConfig, boolean notify) {
        updateForPlayer(sp.getUniqueId(), gameConfig, notify);
    }
    public void updateForPlayer(UUID uuid, GameConfig gameConfig, boolean notify) {
        var playerStats = PLAYER_STATS_MGR.getPlayerStatsByUUID(uuid);
        if (playerStats == null)
            return;

        var gameStats = playerStats.allGameStats.get(gameConfig);
        if (gameStats == null)
            return;

        var normalPB = gameStats.getNormalVariantPB();
        if (normalPB != null) {
            if (!leaderboards.containsKey(gameConfig))
                leaderboards.put(gameConfig, new Leaderboard(gameConfig));

            var opNewIdx = leaderboards.get(gameConfig).add(
                    new Leaderboard.Entry(
                            playerStats.playerUUID,
                            normalPB
                    )
            );
            if (opNewIdx.isPresent() && notify) {
                var player = SERVER.getPlayer(uuid);
                if (player != null) {
                    int lbNum = opNewIdx.get() + 1;
                    Display.sendMsg(
                            player,
                            "&lYou are now {} on the {} leaderboard!",
                            "&f#&a" + lbNum,
                            "&6" + gameConfig.properName
                    );
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 2.0f);

                    for (var otherPlayer : SERVER.getOnlinePlayers()) {
                        if (otherPlayer.equals(player))
                            continue;

                        if (lbNum <= 3) {
                            Display.sendMsg(
                                    player,
                                    Display.format(
                                            "{} is now {} on the {} leaderboard!",
                                            player,
                                            "#&a&l" + lbNum,
                                            "&6" + gameConfig.properName
                                    )
                            );

                            if (lbNum == 1)
                                otherPlayer.playSound(otherPlayer.getLocation(), Sound.LEVEL_UP, 0.3f, 1.0f);
                        }
                    }
                }
            }
        }
    }
}
