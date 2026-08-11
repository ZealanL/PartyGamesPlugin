package zealan.pgp;

import org.bukkit.Server;
import zealan.pgp.api.command.CommandSys;
import zealan.pgp.essentials.Essentials;
import zealan.pgp.game.GameMgr;
import zealan.pgp.party.PartyMgr;
import zealan.pgp.stats.PlayerStatsMgr;
import zealan.pgp.worldevents.WorldEventsMgr;

import java.util.logging.Logger;

public class Globals {
    public static PartyGamesPlugin PLUGIN;
    public static Logger PLOG;
    public static Server SERVER;

    public static CommandSys COMMAND_SYS;
    public static Essentials ESSENTIALS;
    public static PartyMgr PARTY_MGR;
    public static WorldEventsMgr WORLD_EVENTS_MGR;
    public static WorldMgr WORLD_MGR;
    public static PlayerMgr PLAYER_MGR;
    public static GameMgr GAME_MGR;
    public static PlayerStatsMgr PLAYER_STATS_MGR;

    private static boolean initialized = false;
    public static void initialize(PartyGamesPlugin plugin) {
        if (!initialized) {
            initialized = true;

            PLUGIN = plugin;
            PLOG = plugin.getLogger();
            SERVER = plugin.getServer();

            COMMAND_SYS = new CommandSys();
            WORLD_EVENTS_MGR = new WorldEventsMgr();
            WORLD_MGR = new WorldMgr();

            ESSENTIALS = new Essentials();
            PARTY_MGR = new PartyMgr();
            PLAYER_MGR = new PlayerMgr();
            GAME_MGR = new GameMgr();
            PLAYER_STATS_MGR = new PlayerStatsMgr();
        } else {
            throw new RuntimeException("Cannot initialize globals twice");
        }
    }
}
