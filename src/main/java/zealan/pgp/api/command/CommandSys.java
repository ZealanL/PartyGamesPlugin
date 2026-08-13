package zealan.pgp.api.command;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import zealan.pgp.AutoListener;
import zealan.pgp.api.display.Display;

import java.util.*;
import java.util.stream.Collectors;

public class CommandSys extends AutoListener {
    private final LinkedHashMap<String, CommandNode> rootNodes = new LinkedHashMap<>();

    public CommandSys() {
        registerHelpCommand();
    }

    private void registerHelpCommand() {
        register(
                CommandNode.make(
                        "help",
                        "Shows a list of commands available to you",
                        ctx -> {

                            StringBuilder msg = new StringBuilder();
                            msg.append("&7 == &a&lAvailable Commands &7==\n");
                            for (Map.Entry<String, CommandNode> entry : rootNodes.entrySet()) {
                                String cmd = entry.getKey();
                                CommandNode node = entry.getValue();
                                if (!cmd.equals(node.name)) {
                                    // Skip aliases
                                    continue;
                                }

                                if (node.opOnly && !ctx.sender.isOp())
                                    continue;

                                String syntax = node.getUsageSyntax();
                                if (!node.children.isEmpty()) {
                                    List<String> childNames = node.children.stream()
                                            .map(c -> c.name).collect(Collectors.toList());
                                    syntax += " &f&o[" + String.join("/", childNames) + "]";
                                }
                                msg.append(" &f" + syntax);
                                if (syntax.length() + node.description.length() > 50) {
                                    msg.append("\n");
                                    msg.append("&7  - &7&o" + node.description);
                                } else {
                                    // Inline it
                                    msg.append("&7 - &7&o" + node.description);
                                }

                                msg.append("\n");
                            }
                            return CommandResult.ok(msg.toString());
                        }
                )
        );
    }

    public void register(CommandNode rootNode) {
        if (rootNode.parent != null)
            throw new IllegalArgumentException("Root node cannot have a parent");

        if (rootNodes.containsKey(rootNode.name))
            throw new IllegalArgumentException("Command root already registered");

        rootNodes.put(rootNode.name, rootNode);

        for (String alias : rootNode.aliases) {
            if (rootNodes.containsKey(alias))
                throw new IllegalArgumentException("Alias already registered");
            rootNodes.put(alias, rootNode);
        }

        // TODO: Child nodes can also have aliases
    }

    private CommandResult tryExecuteRecursive(CommandCtx ctx, CommandNode currentNode, String[] args) {
        if (currentNode.opOnly && !ctx.sender.isOp())
            return CommandResult.failure("No permission.");

        if (args.length > 0) {
            String subCommandInput = args[0].toLowerCase();

            for (CommandNode child : currentNode.children) {
                boolean matchesName = child.name.equalsIgnoreCase(subCommandInput);
                boolean matchesAlias = child.aliases.stream().anyMatch(a -> a.equalsIgnoreCase(subCommandInput));

                if (matchesName || matchesAlias) {
                    // Match found! Consume args[0] and recursively process the child node
                    String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);
                    return tryExecuteRecursive(ctx, child, remainingArgs);
                }
            }
        }

        if (!currentNode.func.isPresent()) {
            return CommandResult.failure(
                    "Unknown sub-command or incomplete arguments." +
                            "\nUsage: " + currentNode.getUsageSyntax()
            );
        }

        List<CommandArg<?>> argSchema = new ArrayList<>(currentNode.arguments.values());
        int argIdx = 0;

        for (CommandArg<?> schema : argSchema) {
            if (argIdx < args.length) {
                try {
                    Object parsedValue = schema.tryParse(args[argIdx]);
                    ctx.setPassedArg(schema.name, parsedValue);
                } catch (CommandSyntaxException e) {
                    return CommandResult.failure("Invalid argument for <" + schema.name + ">: " + e.getMessage());
                }
                argIdx++;
            } else if (!schema.isOptional()) {
                return CommandResult.failure(
                        "Missing required argument: <" + schema.name + ">" +
                                "\nUsage: " + currentNode.getUsageSyntax()
                );
            }
        }

        // 5. Execute command function
        try {
            return currentNode.onExecute(ctx);
        } catch (CommandSyntaxException e) {
            return CommandResult.failure(
                    "Syntax error!" +
                            "\nUsage: " + currentNode.getUsageSyntax()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.failure("An unexpected error occurred while executing this command! This is bad!");
        }
    }

    private CommandResult onPlayerExecute(Player player, String commandString) {
        while (commandString.startsWith("/"))
            commandString = commandString.substring(1);

        CommandCtx ctx = new CommandCtx(player);
        String[] parts = commandString.trim().split("\\s+");
        if (parts.length < 1 || parts[0].isEmpty())
            return CommandResult.failure("Where's the command? Did you drop it?");

        String root = parts[0].toLowerCase();
        if (root.startsWith("/"))
            root = root.substring(1);

        String rootName = root.toLowerCase();
        CommandNode currentNode = rootNodes.get(rootName);
        if (currentNode == null) {
            if (ctx.sender.isOp())
                return null; // For passthrough to vanilla commands
            return CommandResult.failure("Unknown command, maybe consider {}?", "&f/help");
        }

        int index = 1;
        while (index < parts.length) {
            String token = parts[index].toLowerCase();
            CommandNode matchingChild = null;

            for (CommandNode child : currentNode.children) {
                boolean nameMatch = child.name.equalsIgnoreCase(token);
                boolean aliasMatch = child.aliases.stream().anyMatch(a -> a.equalsIgnoreCase(token));

                if (nameMatch || aliasMatch) {
                    matchingChild = child;
                    break;
                }
            }

            if (matchingChild != null) {
                currentNode = matchingChild;
                index++;
            } else {
                break;
            }
        }

        if (currentNode.opOnly && !ctx.sender.isOp()) {
            return CommandResult.failure("Not allowed!");
        }

        if (!currentNode.func.isPresent()) {
            return CommandResult.failure("Command incomplete, usage: &f" + currentNode.getUsageSyntax());
        }

        List<CommandArg<?>> argSchemas = new ArrayList<>(currentNode.arguments.values());

        for (CommandArg<?> schema : argSchemas) {
            if (index < parts.length) {
                try {
                    Object parsedValue = schema.tryParse(parts[index]);
                    ctx.setPassedArg(schema.name, parsedValue);
                } catch (CommandSyntaxException e) {
                    return CommandResult.failure(
                            "Invalid argument!" +
                                    "\n&cUsage: &f" + currentNode.getUsageSyntax()
                    );
                }
                index++;
            } else if (!schema.isOptional()) {
                return CommandResult.failure(
                        "Missing required argument!" +
                                "\n&cUsage: &f" + currentNode.getUsageSyntax()
                );
            }
        }

        try {
            return currentNode.onExecute(ctx);
        } catch (CommandSyntaxException e) {
            return CommandResult.failure("Syntax Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.failure("An unexpected error occurred while executing the command.");
        }
    }

    public boolean playerExecute(Player player, String commandString) {
        CommandResult result = onPlayerExecute(player, commandString);
        if (result != null) {
            if (result.msg != null)
                Display.sendMsg(player, result.msg);
            return true;
        } else {
            return false;
        }
    }

    @EventHandler
    public void handle(PlayerCommandPreprocessEvent event) {
        if (playerExecute(event.getPlayer(), event.getMessage()))
            event.setCancelled(true);
    }
}
