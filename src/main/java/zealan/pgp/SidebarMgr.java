package zealan.pgp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.GameStyle;
import zealan.pgp.game.Gamer;

import java.util.*;

import static zealan.pgp.Globals.GAME_MGR;

public class SidebarMgr extends AutoListener {

    private record ScoreboardData(String title, List<String> lines) {
    }

    private static final String OBJECTIVE_NAME = "pg_sidebar";
    private final Map<UUID, ScoreboardData> cachedData = new HashMap<>();

    @Override
    public void onTick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            List<String> lines = new ArrayList<>();

            var game = GAME_MGR.getGameFromPlayer(player);

            if (game != null) {
                lines.add("&e&l" + game.config.properName);

                var gamePlayer = game.getPlayingGamer(player);
                if (gamePlayer != null) {
                    if (!game.hasStarted()) {
                        lines.add("&a&oStarting...");
                    } else {
                        var gameStyle = game.config.style;
                        if (gameStyle == GameStyle.POINTS) {
                            var playersByScore = new ArrayList<>(game.getGamers());
                            playersByScore.sort(Comparator.comparingInt(Gamer::getPoints).reversed());

                            for (int i = 0; i < 3 && i < playersByScore.size(); i++) {
                                var otherPlayer = playersByScore.get(i);
                                lines.add("&f#" + (i + 1) + "&7 &e" + otherPlayer.player.getName() + "&7: &b" + otherPlayer.getPoints());
                            }
                        }

                        if (gameStyle != GameStyle.SURVIVAL) {
                            lines.add("&fTime left: &7(&f" +
                                    Display.format(new Display.TicksTime(game.getTicksRemaining(), false)) +
                                    "&7)"
                            );
                        }

                        if (gameStyle == GameStyle.SURVIVAL) {
                            if (gamePlayer.isPlaying()) {
                                lines.add("&fSurvived: " +
                                        Display.format(new Display.TicksTime(game.getTicksElapsed(), false))
                                );
                            }
                            lines.add("&fPlayers: " + game.getPlayingGamers().size() + "&7/" + game.getGamers().size());
                        }
                    }
                } else {
                    lines.add("&7(Not playing)");
                }
            } else {
                lines.add("&7(Not playing)");
            }

            ScoreboardData newData = new ScoreboardData("&lParty Games", lines);
            updateForPlayer(player, newData);
        }
    }

    private void updateForPlayer(Player player, ScoreboardData data) {
        UUID uuid = player.getUniqueId();
        ScoreboardData existing = cachedData.get(uuid);

        Scoreboard board = player.getScoreboard();
        boolean freshBoard = false;

        if (board == null || board.equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
            freshBoard = true;
        }

        if (freshBoard) {
            existing = null;
        }

        Objective objective = board.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = board.registerNewObjective(OBJECTIVE_NAME, "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        String formattedTitle = ChatColor.translateAlternateColorCodes('&', data.title());
        if (!objective.getDisplayName().equals(formattedTitle)) {
            objective.setDisplayName(formattedTitle);
        }

        List<String> newLines = data.lines();
        List<String> oldLines = (existing != null) ? existing.lines() : Collections.emptyList();

        int newSize = newLines.size();
        int oldSize = oldLines.size();

        if (newSize < oldSize) {
            for (int i = newSize; i < oldSize; i++) {
                String entryKey = makeEntryKey(i);
                board.resetScores(entryKey);

                Team team = board.getTeam("sb_line_" + i);
                if (team != null)
                    team.unregister();
            }
        }

        for (int i = 0; i < newSize; i++) {
            String rawText = newLines.get(i);
            String entryKey = makeEntryKey(i);
            int score = newSize - i;

            objective.getScore(entryKey).setScore(score);

            boolean textUnchanged = existing != null && i < oldSize && oldLines.get(i).equals(rawText);
            if (textUnchanged) {
                continue;
            }

            String formattedText = Display.format(rawText);

            Team team = board.getTeam("sb_line_" + i);
            if (team == null) {
                team = board.registerNewTeam("sb_line_" + i);
                team.addEntry(entryKey);
            }

            String[] split = smartSplitLine(formattedText);
            team.setPrefix(split[0]);
            team.setSuffix(split[1]);
        }

        cachedData.put(uuid, data);
    }

    @EventHandler
    void handle(PlayerQuitEvent event) {
        cachedData.remove(event.getPlayer().getUniqueId());
    }

    private String makeEntryKey(int index) {
        return ChatColor.COLOR_CHAR + Integer.toHexString(index) + ChatColor.RESET;
    }

    private String[] smartSplitLine(String text) {
        String prefix = limitLength(text);
        String remainder = text.substring(prefix.length());

        if (remainder.isEmpty()) {
            return new String[]{prefix, ""};
        }

        String lastColors = ChatColor.getLastColors(prefix);
        String suffixSource = lastColors + remainder;
        String suffix = limitLength(suffixSource);

        return new String[]{prefix, suffix};
    }

    private String limitLength(String text) {
        if (text.length() <= 16) {
            return text;
        }

        int cut = 16;
        if (text.charAt(cut - 1) == ChatColor.COLOR_CHAR) {
            cut -= 1;
        }

        return text.substring(0, cut);
    }
}