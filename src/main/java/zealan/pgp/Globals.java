package zealan.pgp;

import org.bukkit.Server;
import zealan.pgp.api.command.CommandSys;
import zealan.pgp.bossbar.BossBarMgr;
import zealan.pgp.essentials.Essentials;
import zealan.pgp.game.GameMgr;
import zealan.pgp.menu.MenuMgr;
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
    public static BossBarMgr BOSS_BAR_MGR;
    public static MenuMgr MENU_MGR;
    public static SidebarMgr SIDEBAR_MGR;

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
            BOSS_BAR_MGR = new BossBarMgr();
            MENU_MGR = new MenuMgr();
            SIDEBAR_MGR = new SidebarMgr();
        } else {
            throw new RuntimeException("Cannot initialize globals twice");
        }
    }
}
