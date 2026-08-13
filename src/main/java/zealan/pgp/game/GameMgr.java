package zealan.pgp.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import zealan.pgp.AutoListener;
import zealan.pgp.api.command.CommandArg;
import zealan.pgp.api.command.CommandNode;
import zealan.pgp.api.command.CommandResult;
import zealan.pgp.api.display.Display;
import zealan.pgp.util.WorldUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static zealan.pgp.Globals.*;

public class GameMgr extends AutoListener {
    private final HashSet<Game> activeGames = new HashSet<>();
    private final WeakHashMap<Player, GameConfig> lastPlayedGames = new WeakHashMap<>();
    private final HashMap<Player, Gamer> gamerMap = new HashMap<>();

    private final AtomicInteger numLoadingGames = new AtomicInteger(0);
    private static final int MAX_LOADING_GAMES = 4;

    public GameMgr() {
        var playVariantRoot = CommandNode.make(
                "play",
                "Play a party game"
        );

        for (var gameConfig : GameConfig.values()) {
            var gameCommandName = gameConfig.snakeCaseName;

            var variants = gameConfig.variants;
            var variantIds = Arrays.stream(variants).map(
                    GameVariant::id
            ).toList();

            var playVariantChild = CommandNode.make(
                    gameCommandName,
                    "Play " + gameConfig.properName,
                    ctx -> {
                        Integer variantIdx = ctx.getArg("variant");

                        if (variantIdx == null) {
                            // If they didn't specify a variant, have them pick one
                            // TODO: Implement
                            return CommandResult.ok();
                        }

                        var variant = variants[variantIdx];
                        var party = PARTY_MGR.getParty(ctx.sender);
                        if (party != null && party.getLeader() != ctx.sender)
                            return CommandResult.failure("Only your party leader can start a game!");

                        GAME_MGR.tryStartGame(ctx.sender, gameConfig, variant);

                        return CommandResult.ok();
                    },
                    new CommandArg.ChoiceArg("variant", variantIds).makeOptional()
            );
            playVariantRoot.withChild(playVariantChild);
        }

        var playAgainRoot = CommandNode.make(
                "playagain",
                "Play the party game you just played",
                ctx -> {
                    GameConfig lastGame = lastPlayedGames.get(ctx.sender);
                    if (lastGame == null)
                        return CommandResult.failure("You haven't played a game since you joined!");

                    ctx.sender.performCommand("play " + lastGame.snakeCaseName);
                    return CommandResult.ok();
                }
        );

        COMMAND_SYS.register(playVariantRoot);
        COMMAND_SYS.register(playAgainRoot);
    }

    public Gamer getGamerFromPlayer(Player player) {
        return gamerMap.get(player);
    }

    public Game getGameFromPlayer(Player player) {
        if (gamerMap.containsKey(player)) {
            return getGamerFromPlayer(player).game;
        } else {
            return null;
        }
    }

    public boolean tryStartGame(Player starter, GameConfig gameConfig, GameVariant variant) {
        var prevGame = getGameFromPlayer(starter);
        if (prevGame != null)
            endGame(prevGame);

        int numGamesAlreadyLoading = numLoadingGames.get();
        if (numLoadingGames.get() >= MAX_LOADING_GAMES) {
            Display.sendMsg(
                    starter,
                    "&cThe server is overloaded trying to start " + numGamesAlreadyLoading + " games!" +
                            "Try again in a sec."
            );
        }

        var party = PARTY_MGR.getParty(starter);
        var playersForGame = new ArrayList<Player>();
        if (party != null) {
            if (party.getLeader() != starter)
                throw new IllegalArgumentException("Only party leaders can start games");
            playersForGame.addAll(party.getMembers());
        } else {
            playersForGame.add(starter);
        }

        var gamers = new ArrayList<Gamer>();
        for (int i = 0; i < playersForGame.size(); i++)
            gamers.add(new Gamer(i, playersForGame.get(i)));

        var aroundPos = gameConfig.loadInfo.spawns[0].getBlockPos();

        Bukkit.getScheduler().runTask(PLUGIN, () -> {
            var startMs = System.currentTimeMillis();
            numLoadingGames.getAndIncrement();

            var world = WORLD_MGR.createTempWorldAround(aroundPos);

            Game game;
            try {
                var constructor = gameConfig.cls.getDeclaredConstructor(Game.InitParams.class);
                game = constructor.newInstance(new Game.InitParams(gameConfig, variant, gamers, world));
            } catch (Exception e) {
                PLOG.severe("Failed to start game " + gameConfig + ", exception instantiating: " + e);
                return;
            }

            for (var gamer : gamers) {
                gamerMap.put(gamer.player, gamer);
                lastPlayedGames.put(gamer.player, gameConfig);
                gamer.setGame(game);
                Display.sendMsg(gamer.player, "&aStarting game {}...", "&6" + gameConfig.properName);
            }
            activeGames.add(game);
            WORLD_EVENTS_MGR.register(world, game);

            game.onLoaded();
            numLoadingGames.getAndDecrement();

            var elapsedMs = System.currentTimeMillis() - startMs;
            PLOG.info("Created new game world in " + elapsedMs + " ms!");
        });

        return true;
    }

    public boolean endGame(Game game) {
        if (game.hasEnded())
            return false;

        game.end();
        return true;
    }

    @EventHandler
    void handle(PlayerQuitEvent quitEvent) {
        if (gamerMap.containsKey(quitEvent.getPlayer())) {
            var gamer = gamerMap.get(quitEvent.getPlayer());
            if (gamer.isPlaying())
                gamer.stopPlaying(false);
            gamerMap.remove(quitEvent.getPlayer());
        }
    }


    @Override
    public void onTick() {
        for (var game : activeGames) {
            game.onTick();

            if (game.hasEnded()) {
                WORLD_EVENTS_MGR.unregister(game.world, game);
                activeGames.remove(game);
                for (var gamer : game.getGamers())
                    gamerMap.remove(gamer.player);
            }

            // Auto-stop-playing anyone who left the world or is a spectator
            for (var gamer : game.getGamers()) {
                if (gamer.isPlaying()) {
                    if (gamer.player.getWorld() != game.world || PLAYER_MGR.isFakeSpectator(gamer.player)) {
                        gamer.stopPlaying(false);
                    }
                }
            }
        }
    }
}
