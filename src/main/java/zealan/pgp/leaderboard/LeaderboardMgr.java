package zealan.pgp.leaderboard;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import zealan.pgp.AutoListener;
import zealan.pgp.api.command.CommandNode;
import zealan.pgp.api.command.CommandResult;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.GameCommandArg;
import zealan.pgp.game.GameConfig;
import zealan.pgp.math.Vec3i;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

import static zealan.pgp.Globals.*;

public class LeaderboardMgr extends AutoListener {
    private final HashMap<GameConfig, Leaderboard> leaderboards = new HashMap<>();
    private final static int SHOWCASE_CYCLE_TICKS = 20 * 5;
    private final static Vec3i SHOWCASE_POS = new Vec3i(23, 116, 199);

    public LeaderboardMgr() {
        COMMAND_SYS.register(
                CommandNode.make(
                        "leaderboard",
                        "Get leaderboard for a game",
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

    @Override
    public void onTick() {
        if (!leaderboards.isEmpty() && getListenerTickCount() % SHOWCASE_CYCLE_TICKS == 0) {
            int showcaseIdx = (getListenerTickCount() / SHOWCASE_CYCLE_TICKS) % leaderboards.size();
            var showcaseBoard = leaderboards.values().stream().toList().get(showcaseIdx);


            var lines = showcaseBoard.toMsgLines();
            lines.add(0, Display.format("&7Use &f/lb &7to view the leaderboard:"));
            ARMOR_STAND_MGR.setAtPos(SHOWCASE_POS, lines);
        }
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
