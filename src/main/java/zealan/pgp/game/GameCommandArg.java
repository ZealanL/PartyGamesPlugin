package zealan.pgp.game;

import zealan.pgp.api.command.CommandArg;
import zealan.pgp.api.command.CommandSyntaxException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

// TODO: Use for "/play"
public class GameCommandArg extends CommandArg<GameConfig> {
    public GameCommandArg() {
        super("game");
    }

    @Override
    public Optional<Collection<String>> getSuggestions() {
        return Optional.of(
                Arrays.stream(GameConfig.values()).map(
                        g -> g.snakeCaseName
                ).toList()
        );
    }

    @Override
    public GameConfig tryParse(String strVal) throws CommandSyntaxException {
        GameConfig possibleGame = null;
        for (var gameConfig : GameConfig.values()) {
            if (gameConfig.snakeCaseName.contains(strVal.toLowerCase())) {
                if (possibleGame == null) {
                    possibleGame = gameConfig;
                } else {
                    throw new CommandSyntaxException("Ambiguous game name");
                }
            }
        }
        if (possibleGame == null)
            throw new CommandSyntaxException("Unknown game name");

        return possibleGame;
    }
}
