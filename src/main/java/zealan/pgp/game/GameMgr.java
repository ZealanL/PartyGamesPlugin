package zealan.pgp.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import zealan.pgp.AutoListener;
import zealan.pgp.api.command.CommandArg;
import zealan.pgp.api.command.CommandNode;
import zealan.pgp.api.command.CommandResult;
import zealan.pgp.api.display.Display;
import zealan.pgp.menu.MenuInv;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static zealan.pgp.Globals.*;

public class GameMgr extends AutoListener {
    private record LastPlayedGame(GameConfig config, GameVariant variant, Instant when) {
    }

    private final Set<Game> activeGames = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Player, LastPlayedGame> lastPlayedGames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Player, Gamer> gamerMap = new ConcurrentHashMap<>();

    private final AtomicInteger numLoadingGames = new AtomicInteger(0);
    private static final int MAX_LOADING_GAMES = 2;

    public GameMgr() {
        var playVariantRoot = CommandNode.make(
                "play",
                "Play a party game",
                ctx -> {
                    var menuItems = new ArrayList<MenuInv.ItemEntry>();
                    for (var gameType : GameConfig.values()) {
                        menuItems.add(new MenuInv.ItemEntry(
                                new ItemStack(gameType.iconItem),
                                "&b&l" + gameType.properName,
                                null,
                                "play " + gameType.snakeCaseName
                        ));
                    }

                    MENU_MGR.showMenuInv(ctx.sender, "Games", menuItems, MenuInv.Size.LARGE);
                    return CommandResult.ok();
                }
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
                            var items = new ArrayList<MenuInv.ItemEntry>();
                            for (var variant : variants) {
                                var itemStack = new ItemStack(variant.iconItem());
                                var itemName = (variant != GameVariant.NONE) ?
                                        "&6" + variant.name() :
                                        "&7" + variant.name();
                                items.add(
                                        new MenuInv.ItemEntry(
                                                itemStack,
                                                itemName,
                                                variant.desc(),
                                                "play " + gameCommandName + " " + variant.id()
                                        )
                                );
                                if (variant == GameVariant.NONE) {
                                    items.add(null);
                                }
                            }

                            MENU_MGR.showMenuInv(
                                    ctx.sender,
                                    "Select Variant",
                                    items,
                                    MenuInv.Size.ROW
                            );
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
                    var lastGame = lastPlayedGames.get(ctx.sender);
                    if (lastGame == null)
                        return CommandResult.failure("You haven't played a game since you joined!");

                    COMMAND_SYS.playerExecute(
                            ctx.sender,
                            "play " + lastGame.config.snakeCaseName + " " + lastGame.variant.id()
                    );
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
            for (var game : activeGames) {
                if (game.getGamer(player) != null) {
                    throw new RuntimeException("Wtf");
                }
            }
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
            return false;
        }

        var lastPlayed = lastPlayedGames.get(starter);
        if (lastPlayed != null) {
            var timeSince = Duration.between(lastPlayed.when, Instant.now());
            if (timeSince.getSeconds() < 2) {
                Display.sendMsg(
                        starter,
                        "&cPlease wait a sec before starting another game!"
                );
                return false;
            }
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

        PLOG.info("Started loading game...");
        int numSpawns = gameConfig.loadInfo.spawns.length;
        ArrayList<Integer> spawnShuffle = IntStream.range(0, numSpawns)
                .boxed().collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(spawnShuffle);

        var gamers = new ArrayList<Gamer>();
        for (int i = 0; i < playersForGame.size(); i++)
            gamers.add(new Gamer(spawnShuffle.get(i % spawnShuffle.size()), playersForGame.get(i)));

        var loadRange = gameConfig.loadInfo.loadRange;
        Bukkit.getScheduler().runTask(PLUGIN, () -> {
            var startMs = System.currentTimeMillis();
            numLoadingGames.getAndIncrement();

            var world = WORLD_MGR.createTempWorldOf(loadRange);

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
                lastPlayedGames.put(gamer.player, new LastPlayedGame(gameConfig, variant, Instant.now()));
                gamer.setGame(game);
                Display.sendMsg(gamer.player, "&aStarting game {}...", "&6" + gameConfig.properName);
            }

            activeGames.add(game);
            WORLD_EVENTS_MGR.register(world, game);

            var elapsedMs = System.currentTimeMillis() - startMs;
            PLOG.info("Created new game world in " + elapsedMs + " ms!");
            game.onLoaded();
            numLoadingGames.getAndDecrement();
            PLOG.info("Finished loading game");
        });

        return true;
    }

    public boolean endGame(Game game) {
        if (game.hasEnded())
            return false;

        PLOG.info("Ending game...");

        game.end();
        WORLD_EVENTS_MGR.unregister(game.world, game);
        for (var gamer : game.getGamers())
            gamerMap.remove(gamer.player);
        activeGames.remove(game);
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
        lastPlayedGames.remove(quitEvent.getPlayer());
    }

    @Override
    public void onTick() {
        for (var game : activeGames) {
            // Auto-stop-playing anyone who left the world or is a spectator
            for (var gamer : game.getGamers()) {
                if (gamer.isPlaying()) {
                    if (gamer.player.getWorld() != game.world || PLAYER_MGR.isFakeSpectator(gamer.player)) {
                        gamer.stopPlaying(false);
                    }
                }
            }

            game.onTick();

            if (game.hasEnded()) {
                try {
                    WORLD_EVENTS_MGR.unregister(game.world, game);
                } catch (IllegalStateException e) {
                    PLOG.warning("Game unregistration failed");
                }
                for (var gamer : game.getGamers())
                    gamerMap.remove(gamer.player);
                activeGames.remove(game);
            }
        }
    }
}
