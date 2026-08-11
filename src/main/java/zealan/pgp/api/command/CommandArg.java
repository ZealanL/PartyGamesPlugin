package zealan.pgp.api.command;


import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

import static zealan.pgp.Globals.SERVER;

public abstract class CommandArg<T> {
    public final String name;
    boolean isOptional = false;

    public CommandArg(String name) {
        CommandUtil.verifyName(name);
        this.name = name;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public CommandArg<?> makeOptional() {
        this.isOptional = true;
        return this;
    }

    public abstract Optional<Collection<String>> getSuggestions();
    public abstract T tryParse(String strVal) throws CommandSyntaxException;

    // ///////

    public static class PlayerArg extends CommandArg<Player> {
        public PlayerArg(String name) {
            super(name);
        }

        @Override
        public Optional<Collection<String>> getSuggestions() {
            List<String> set = SERVER.getOnlinePlayers().stream().map(
                    HumanEntity::getName
            ).collect(Collectors.toList());
            return Optional.of(set);
        }

        @Override
        public Player tryParse(String strVal) throws CommandSyntaxException {
            Player player = SERVER.getPlayer(strVal);
            if (player != null) {
                return player;
            } else {
                throw new CommandSyntaxException("Invalid player name");
            }
        }
    }

    public static class EnumArg<E extends java.lang.Enum<E>> extends CommandArg<E> {
        private final HashMap<String, E> suggestionsCache = new HashMap<>();
        private final HashMap<String, E> suggestionsCacheLower = new HashMap<>();
        public EnumArg(String name, Class<E> enumClass) {
            super(name);
            for (E e : enumClass.getEnumConstants()) {
                suggestionsCache.put(e.name(), e);
                suggestionsCacheLower.put(e.name().toLowerCase(), e);
            }
        }

        @Override
        public Optional<Collection<String>> getSuggestions() {
            return Optional.of(suggestionsCache.keySet());
        }

        @Override
        public E tryParse(String strVal) throws CommandSyntaxException {
            E result = suggestionsCacheLower.get(strVal.toLowerCase());
            if (result != null) {
                return result;
            } else {
                throw new CommandSyntaxException("Invalid enum variant");
            }
        }
    }

    public static class ChoiceArg extends CommandArg<Integer> {
        private final List<String> choices;

        public ChoiceArg(String name, Collection<String> choices) {
            super(name);
            this.choices = new ArrayList<>(choices);
        }

        @Override
        public Optional<Collection<String>> getSuggestions() {
            return Optional.of(choices);
        }

        @Override
        public Integer tryParse(String strVal) throws CommandSyntaxException {
            for (int i = 0; i < choices.size(); i++)
                if (choices.get(i).equalsIgnoreCase(strVal))
                    return i;

            throw new CommandSyntaxException("Invalid choice");
        }
    }

    public static class AnyString extends CommandArg<String> {
        public AnyString(String name) {
            super(name);
        }

        @Override
        public Optional<Collection<String>> getSuggestions() {
            return Optional.empty();
        }

        @Override
        public String tryParse(String strVal) throws CommandSyntaxException {
            return strVal;
        }
    }
}
