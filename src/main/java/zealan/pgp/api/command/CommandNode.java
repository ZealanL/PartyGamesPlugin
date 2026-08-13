package zealan.pgp.api.command;

import zealan.pgp.api.display.Display;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Optional;

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
        String prefix;
        {
            var sb = new StringBuilder(this.name);
            for (CommandNode parent = this.parent; parent != null; parent = parent.parent)
                sb.insert(0, parent.name + " ");
            sb.insert(0, "&7/&f");
            prefix = sb.toString();
        }

        var lines = new ArrayList<String>();

        if (this.func.isPresent()) {
            var sb = new StringBuilder(prefix);
            for (CommandArg<?> arg : this.arguments.values()) {
                if (arg.isOptional) {
                    sb.append(" &7[&7");
                    sb.append(arg.name);
                    sb.append("&7]");
                } else {
                    sb.append(" &7<&6");
                    sb.append(arg.name);
                    sb.append("&7>");
                }
            }
            lines.add(sb.toString());
        }

        if (!this.children.isEmpty()) {
            var sb = new StringBuilder(prefix);
            sb.append(" &9[");
            sb.append(
                    String.join(" / ",
                        this.children.stream().map(
                                ch -> "&7" + ch.name
                        ).toList()
                    )
            );
            sb.append("&9]");
            lines.add(sb.toString());
        }

        return Display.concatLines(lines.toArray(String[]::new));
    }
}

