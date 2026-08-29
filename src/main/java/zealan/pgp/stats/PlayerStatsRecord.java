package zealan.pgp.stats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.*;

import java.time.Instant;
import java.util.*;

import static zealan.pgp.Globals.LEADERBOARD_MGR;
import static zealan.pgp.Globals.PLOG;

public class PlayerStatsRecord {
    public final UUID playerUUID;
    public String playerName;

    public int numJoins = 0;
    public int numLeaves = 0;

    public static class GamePB {
        public final GameRecordStyle type;
        public final int val; // Number of points OR ticks played for
        public final long time;

        GamePB(GameConfig gameCfg, Gamer player) {
            this.type = gameCfg.recordStyle;
            this.val = switch (gameCfg.recordStyle) {
                case POINTS -> player.getScore();
                case LONGEST_TIME, FASTEST_TIME -> player.getTicksPlayedFor();
            };

            this.time = Instant.now().getEpochSecond();
        }

        public boolean isBetterThan(GamePB other) {
            if (this.type != other.type)
                throw new IllegalArgumentException("PB types are different");

            return switch (this.type) {
                case POINTS, LONGEST_TIME -> this.val > other.val;
                case FASTEST_TIME -> this.val < other.val;
            };
        }

        public String toMsgPart() {
            Object valDisplay = switch (this.type) {
                case POINTS -> Display.format(this.val);
                case FASTEST_TIME, LONGEST_TIME -> new Display.TicksTime(this.val, true);
            };

            return Display.format("{}", valDisplay);
        }

        public static int compare(GamePB a, GamePB b) {
            if (a.isBetterThan(b)) return -1;
            if (b.isBetterThan(a)) return 1;
            return Long.compare(a.time, b.time);
        }
    }

    public static class GameStats {
        public final GameConfig gameConfig;
        public int totalTicksPlayed = 0;
        public int timesPlayed = 0;

        public GameStats(GameConfig gameConfig) {
            this.gameConfig = gameConfig;
        }

        public Map<String, GamePB> variantPBs = new TreeMap<>(
                (a, b) -> {
                    // Force "NONE" variant to come first, always
                    a = a.replace(GameVariant.NONE.name(), " ");
                    b = b.replace(GameVariant.NONE.name(), " ");

                    return a.compareToIgnoreCase(b);
                }
        );

        boolean hasPB() {
            return !variantPBs.isEmpty();
        }

        String toPrintOutText(UUID playerUUID) {
            var dataMap = new LinkedHashMap<String, Object>();
            dataMap.put("&7Times played", this.timesPlayed);
            dataMap.put("&7Total playtime", new Display.TicksTime(this.totalTicksPlayed, false));

            for (var pbEntry : variantPBs.entrySet()) {
                var valueText = pbEntry.getValue().toMsgPart();

                boolean isNormalVariant = pbEntry.getKey().equals(GameVariant.NONE.name());
                if (isNormalVariant) {
                    var lb = LEADERBOARD_MGR.getLeaderboard(gameConfig);
                    if (lb != null) {
                        int lbNumber = 0;
                        for (int i = 0; i < lb.size(); i++) {
                            if (lb.get(i).playerUUID().equals(playerUUID)) {
                                lbNumber = i + 1;
                                break;
                            }
                        }

                        if (lbNumber > 0)
                            valueText += Display.format(" &8[&f#&e" + lbNumber + "&8]");
                    }

                }

                dataMap.put("&f&lPB&r (" + pbEntry.getKey() + ")", valueText);
            }

            return mapToMsg(
                    dataMap, "&a&l" + gameConfig.properName, 1
            );
        }

        public static int compare(GamePB a, GamePB b) {
            if (a.isBetterThan(b)) return -1;
            if (b.isBetterThan(a)) return 1;
            return 0;
        }

        public GamePB getNormalVariantPB() {
            return variantPBs.get(GameVariant.NONE.name());
        }
    }

    public final Map<GameConfig, GameStats> allGameStats = new TreeMap<>(
            Comparator.comparing(a -> a.properName)
    );

    public PlayerStatsRecord(Player player) {
        this.playerUUID = player.getUniqueId();
        this.playerName = player.getName();
    }

    GameStats getGameStats(GameConfig gameConfig) {
        return allGameStats.get(gameConfig);
    }

    public void syncPlayerName() {
        var offlinePlayer = Bukkit.getOfflinePlayer(this.playerUUID);
        if (offlinePlayer.hasPlayedBefore())
            this.playerName = offlinePlayer.getName();
    }

    public void updateAfterPlaying(Gamer gamer, Game game, boolean finished) {
        if (!gamer.player.getUniqueId().equals(this.playerUUID)) {
            PLOG.warning(
                    "PlayerStatsRecord: Player UUID mismatch" +
                            " (" + gamer.player.getUniqueId() + " != " + this.playerUUID + ")"
            );
        }

        if (gamer.isPlaying())
            throw new RuntimeException("Player is still playing");

        syncPlayerName();

        allGameStats.putIfAbsent(game.config, new GameStats(game.config));
        var gameStats = allGameStats.get(game.config);
        gameStats.timesPlayed++;
        gameStats.totalTicksPlayed += gamer.getTicksPlayedFor();

        if (finished && game.config.recordStyle != null) {
            var variant = game.variant;
            var existingPB = gameStats.variantPBs.get(variant.name());
            var candidatePB = new GamePB(game.config, gamer);
            if (existingPB == null) {
                gameStats.variantPBs.put(variant.name(), candidatePB);
            } else {
                if (candidatePB.isBetterThan(existingPB)) {
                    gameStats.variantPBs.put(variant.name(), candidatePB);

                    Display.sendMsg(gamer.player,
                            "&aYou set a &LNEW PB &ain {}&7: {}",
                            "&6&l" + game.config.properName,
                            "&b" + candidatePB.toMsgPart()
                    );
                }
            }
        }
    }

    public int calcTotalGamesPlayed() {
        int sum = 0;
        for (var val : allGameStats.values())
            sum += val.timesPlayed;
        return sum;
    }

    private static String mapToMsg(Map<String, Object> map, String title, int indentLevel) {
        var pad = " ".repeat(indentLevel * 2);
        StringBuilder sb = new StringBuilder(pad + title + "&7: ");

        for (var entry : map.entrySet()) {
            var k = entry.getKey();
            var v = entry.getValue();
            sb.append(
                    Display.format("\n &7- &f{}&7: {}", k, v)
            );
        }

        if (map.isEmpty())
            sb.append("\n&7  (none)");

        return sb.toString();
    }

    public String toPrintOutMsg() {
        var offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        var firstPlayedMilli = offlinePlayer.getFirstPlayed();
        var firstPlayedSecs = firstPlayedMilli / 1000;

        StringBuilder sb = new StringBuilder();
        sb.append(Display.format("&7-- &5&lStats for {} &7--", "&e&l" + playerName));
        sb.append("\n").append(mapToMsg(
                new HashMap<>(Map.of(
                        "Total joins", numJoins,
                        "First join date", new Display.UnixTime(firstPlayedSecs),
                        "Total games played", calcTotalGamesPlayed()
                )),
                "&dBase stats", 0
        ));


        for (var gameEntry : allGameStats.entrySet())
            sb.append("\n").append(gameEntry.getValue().toPrintOutText(playerUUID));

        return Display.format(
                "{}\n{}\n{}",
                Display.BAR,
                sb.toString(),
                Display.BAR
        );
    }

    // ///////

    public void fixInvalid() {

        allGameStats.entrySet().removeIf((entry) -> {
            var gameConfig =  entry.getKey();
            var gameStats = entry.getValue();
            if (gameConfig == null || gameStats.gameConfig == null) {
                PLOG.warning("Removed invalid game stats entry (null config)");
                return true;
            } else {
                return false;
            }
        });

        for (var gameConfig : GameConfig.values()) {
            var pb = allGameStats.get(gameConfig);
            if (pb == null)
                continue;
            pb.variantPBs.entrySet().removeIf(
                    (entry) -> {
                        String variantName = entry.getKey();
                        GamePB gamePB = entry.getValue();
                        if (gamePB.type != gameConfig.recordStyle) {
                            PLOG.warning(
                                    "Removing invalid \"" + gameConfig.properName + "\" PB record" +
                                            " for \"" + playerName + "\" due to wrong style type "
                            );
                            return true;
                        } else {
                            return false;
                        }
                    }
            );
        }
    }
}
