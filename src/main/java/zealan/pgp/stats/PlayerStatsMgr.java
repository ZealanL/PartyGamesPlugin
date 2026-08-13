package zealan.pgp.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import zealan.pgp.AutoListener;
import zealan.pgp.api.command.CommandArg;
import zealan.pgp.api.command.CommandNode;
import zealan.pgp.api.command.CommandResult;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import static zealan.pgp.Globals.*;

public class PlayerStatsMgr extends AutoListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();

    private static final int STATS_FILE_VERSION = 2;
    private static final Path JSON_PATH = PLUGIN.getDataFolder().toPath().resolve("stats_v" + STATS_FILE_VERSION + ".json");

    private static final int SAVE_INTERVAL_TICKS = 120 * 20;
    private int ticksSinceSave = 0;
    private final HashMap<UUID, PlayerStatsRecord> records = new HashMap<>();

    public PlayerStatsMgr() {
        COMMAND_SYS.register(CommandNode.make("stats", "Display a player's stats", (ctx) -> {
            String playerName = ctx.getArg("player_name");
            if (playerName == null) playerName = ctx.sender.getName();

            var stats = getPlayerStatsByName(playerName);
            if (stats == null) return CommandResult.failure("No stats are recorded for \"" + playerName + "\"");

            // TODO: Book implementation
                    /*
                    var book = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WRITTEN_BOOK);
                    var bookMeta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();

                    bookMeta.setTitle("Player Stats");
                    bookMeta.setAuthor("[SERVER]");
                    bookMeta.addPages(
                            stats.toPrintOutText().toAdventureComponent()
                    );

                    book.setItemMeta(bookMeta);
                    ctx.sender.getBukkitEntity().openBook(book);
                    */

            return CommandResult.ok(stats.toPrintOutMsg());

        }, new CommandArg.AnyString("player_name").makeOptional()));
        COMMAND_SYS.register(CommandNode.make("reloadstats", "Reload all player stats", ctx -> {
            this.load();
            // TODO: ADD
            //LEADERBOARD_MGR.fullUpdate();
            return CommandResult.ok("Reloaded!");
        }).withOpOnly());

        load();
    }

    private void load() {
        if (!Files.exists(JSON_PATH)) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(JSON_PATH.toFile())) {
            Type type = new TypeToken<HashMap<UUID, PlayerStatsRecord>>() {
            }.getType();
            HashMap<UUID, PlayerStatsRecord> loadedRecords = GSON.fromJson(reader, type);

            records.clear();
            if (loadedRecords != null) records.putAll(loadedRecords);

            PLOG.info("PlayerStatsManager(): Loaded stats for " + records.size() + " players");
        } catch (IOException e) {
            PLOG.severe("PlayerStatsManager(): FAILED to load player stats: " + e);
        }
    }

    private void save() {
        ticksSinceSave = 0;

        try {
            Files.createDirectories(JSON_PATH.getParent());
            try (FileWriter writer = new FileWriter(JSON_PATH.toFile())) {
                GSON.toJson(records, writer);
            }
            PLOG.info("PlayerStatsManager(): Saved stats for " + records.size() + " players");
        } catch (IOException e) {
            PLOG.severe("PlayerStatsManager(): FAILED to save all player stats: " + e);
        }
    }

    public Collection<PlayerStatsRecord> getAllRecords() {
        return records.values();
    }

    public PlayerStatsRecord getPlayerStats(Player player) {
        return getPlayerStatsByUUID(player.getUniqueId());
    }

    public PlayerStatsRecord getPlayerStatsByUUID(UUID uuid) {
        return records.get(uuid);
    }

    public PlayerStatsRecord getPlayerStatsByName(String playerName) {
        for (var entry : records.entrySet())
            if (entry.getValue().playerName.equalsIgnoreCase(playerName)) return entry.getValue();

        return null;
    }

    public void onPlayerStoppedPlaying(Gamer gp, Game game, boolean finished) {
        if (gp.isPlaying()) throw new RuntimeException("Player has not finished");

        var record = getPlayerStats(gp.player);
        record.updateAfterPlaying(gp, game, finished);

        // TODO: ADD
        //Bukkit.getScheduler().runTaskLater(PLUGIN, () -> {
        //    LEADERBOARD_MGR.updateForPlayer(gp.ent, game.getConfig(), true);
        //}, 2 * 20);
    }

    // ///////////

    @EventHandler
    protected void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var record = getPlayerStats(player);
        if (record == null) {
            record = new PlayerStatsRecord(player);
            records.put(player.getUniqueId(), record);
        }

        record.numJoins++;
    }

    @EventHandler
    protected void onPlayerLeave(PlayerQuitEvent event) {
        var player = event.getPlayer();
        var record = getPlayerStats(player);
        if (record == null) throw new RuntimeException("Player left with no stats accessible!?");

        record.numLeaves++;
    }

    @Override
    public void onTick() {
        ticksSinceSave++;
        if (ticksSinceSave >= SAVE_INTERVAL_TICKS) save();
    }
}
