package zealan.pgp.leaderboard;

import zealan.pgp.api.display.Display;
import zealan.pgp.game.GameConfig;
import zealan.pgp.stats.PlayerStatsRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static zealan.pgp.Globals.PLAYER_STATS_MGR;
import static zealan.pgp.Globals.PLOG;

public class Leaderboard {
    public static final int MAX_ENTRIES = 5;
    public final GameConfig gameConfig;

    public record Entry(UUID playerUUID, PlayerStatsRecord.GamePB gamePB) {}

    private final List<Entry> entries = new ArrayList<>();

    public Leaderboard(GameConfig gameConfig) {
        this.gameConfig = gameConfig;
    }

    Optional<Integer> add(Entry entry) {
        for (var existing : entries) {
            if (existing.playerUUID().equals(entry.playerUUID()) && !entry.gamePB().isBetterThan(existing.gamePB())) {
                return Optional.empty();
            }
        }

        if (entries.size() >= MAX_ENTRIES && !entry.gamePB().isBetterThan(entries.getLast().gamePB())) {
            return Optional.empty();
        }

        entries.removeIf(existing -> existing.playerUUID().equals(entry.playerUUID()));
        entries.add(entry);
        entries.sort((a, b) -> PlayerStatsRecord.GamePB.compare(a.gamePB(), b.gamePB()));
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }

        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).playerUUID().equals(entry.playerUUID())) {
                return Optional.of(i);
            }
        }

        return Optional.empty();
    }

    public int size() {
        return entries.size();
    }

    public Entry get(int index) {
        return entries.get(index);
    }

    public List<String> toMsgLines() {
        var lines = new ArrayList<String>();

        lines.add(
                Display.format("&7== &6&l" + gameConfig.properName + " &7==")
        );

        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            PlayerStatsRecord stats = PLAYER_STATS_MGR.getPlayerStatsByUUID(entry.playerUUID);
            if (stats == null) {
                PLOG.warning("Stats is null for player on leaderboards? UUID = " + entry.playerUUID());
                continue;
            }

            var playerName = stats.playerName;
            lines.add(Display.format(
                    "&7#{}&7. {} &7by {}",
                    "&a&l" + (i + 1),
                    entry.gamePB.toMsgPart(),
                    "&e&l" + playerName
            ));
        }
        return lines;
    }
}
