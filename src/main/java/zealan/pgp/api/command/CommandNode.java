package zealan.pgp.api.command;

import java.util.*;

public class CommandNode {
    public final String name;
    public final String description;
    public final HashSet<CommandNode> children;
    public CommandNode parent = null;
    public final LinkedHashMap<String, CommandArg<?>> arguments;
    public final Optional<CommandFunc> func;
    public final HashSet<String> aliases;
    public boolean opOnly = false;

    private CommandNode(String name, String description, Optional<CommandFunc> func, CommandArg<?>... args) {
        CommandUtil.verifyName(name);

        this.name = name;
        this.description = description;
        this.children = new HashSet<>();
        this.aliases = new HashSet<>();

        this.arguments = new LinkedHashMap<>();
        for (CommandArg<?> arg : args) {
            if (arguments.containsKey(arg.name))
                throw new RuntimeException("Duplicate argument name: \"" + arg.name + "\"");
            arguments.put(arg.name, arg);
        }

        this.func = func;
    }

    public static CommandNode make(String name, String description, CommandFunc func, CommandArg<?>... args) {
        return new CommandNode(name, description, Optional.of(func), args);
    }

    public static CommandNode make(String name, String description, CommandArg<?>... args) {
        return new CommandNode(name, description, Optional.empty(), args);
    }

    public CommandNode withChild(CommandNode node) {
        this.children.add(node);
        node.parent = this;
        return this;
    }

    public CommandNode withAlias(String alias) {
        CommandUtil.verifyName(alias);
        if (this.aliases.contains(alias))
            throw new RuntimeException("Duplicate alias: \"" + alias + "\"");
        if (alias.equals(name))
            throw new RuntimeException("Alias identical to name: \"" + name + "\"");
        this.aliases.add(alias);
        return this;
    }

    public CommandNode withOpOnly() {
        this.opOnly = true;
        return this;
    }

    CommandResult onExecute(CommandCtx ctx) throws CommandSyntaxException {
        return this.func.get().execute(ctx);
    }

    String getUsageSyntax() {
        StringBuilder result = new StringBuilder(this.name);
        for (CommandNode parent = this.parent; parent != null; parent = parent.parent)
            result.insert(0, parent.name + " ");
        result.insert(0, "&7/&f");

        for (CommandArg<?> arg : this.arguments.values()) {
            if (arg.isOptional) {
                result.append(" &7[&7");
                result.append(arg.name);
                result.append("&7]");
            } else {
                result.append(" &7<&6");
                result.append(arg.name);
                result.append("&7>");
            }

        }

        return result.toString();
    }
}

