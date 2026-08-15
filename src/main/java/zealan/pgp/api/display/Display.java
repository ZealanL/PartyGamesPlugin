package zealan.pgp.api.display;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static zealan.pgp.Globals.PLOG;

public class Display {
    public static String BAR = "&7" + ("=".repeat(40));
    public record TicksTime(int numTicks, boolean showDecimals) {
        @Override
        public @NotNull String toString() {
            double secs = (numTicks % (60 * 20)) / 20.0;
            int mins = numTicks / (60 * 20);
            String s;
            if (showDecimals) {
                s = MessageFormat.format("{0,number}:{1,number,00.00}", mins, secs);
            } else {
                s = MessageFormat.format("{0,number,00}:{1,number,00}", mins, secs);
            }

            while (showDecimals && s.length() > 1 && (s.charAt(0) == '0' ||  s.charAt(0) == ':'))
                s = s.substring(1);
            return "&b" + s.replace(":", "&f:&b");
        }
    }
    public record UnixTime(long secs) {
        @Override
        public @NotNull String toString() {
            var instant = Instant.ofEpochSecond(secs);
            var zdt = instant.atZone(ZoneId.systemDefault());

            String formatted;
            if (zdt.getYear() != Year.now().getValue()) {
                formatted = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
                        .format(instant);
            } else {
                String monthStr = zdt.getMonth().getDisplayName(TextStyle.SHORT, Locale.US) + ".";
                formatted = monthStr + " " + zdt.getDayOfMonth();
            }

            return "&b" + formatted;
        }
    }

    public static String format(Object arg) {
        if (arg instanceof Entity e) {
            return ("&e" + e.getName());
        } else if (arg instanceof String str) {
            return str.replace('&', ChatColor.COLOR_CHAR);
        } else if (arg instanceof Double || arg instanceof Float) {
            var asDouble = (double)arg;
            return String.format("&b%.2f", asDouble);
        } else {
            return "&b" + arg;
        }
    }

    public static String format(String base, Object... args) {
        var stringArgs = new ArrayList<String>();
        for (Object arg : args) {
            stringArgs.add(format(arg));
        }

        String[] basePieces = base.split("\\{\\}", -1);
        if (basePieces.length != stringArgs.size() + 1) {
            PLOG.severe("format() given wrong arguments for \"" + base + "\"");
            return "&c[ERROR]";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < basePieces.length; i++) {
            String piece = basePieces[i];
            result.append(piece);
            if (i < basePieces.length - 1) {
                result.append(stringArgs.get(i));

                // Restore formatting after
                result.append(FormatState.parse(piece));
            }
        }
        String resultStr = result.toString();
        return resultStr.replace('&', ChatColor.COLOR_CHAR);
    }

    public static String concatLines(String...lines) {
        return String.join(
                "\n",
                Arrays.stream(lines)
                        .map(Display::format).toArray(String[]::new)
        );
    }

    public static String concatLines(List<String> lines) {
        return concatLines(lines.toArray(String[]::new));
    }

    public static void sendMsg(Player player, String message, Object ... args) {
        player.sendMessage(format(message, args));
    }

    public static void sendCommandMsg(Player player, String command, String commandHint, String message, Object ... args) {
        String msg = format(message, args);
        TextComponent comp = new TextComponent(msg);
        comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command));
        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(commandHint).create()));
        player.spigot().sendMessage(comp);
    }

    public static void sendMsg(World world, String message, Object ... args) {
        String msg = format(message, args);
        for (Player player : world.getPlayers())
            player.sendMessage(msg);
    }

    public static void sendGlobalMsg(String message, Object ... args) {
        String msg = format(message, args);
        for (Player player : Bukkit.getOnlinePlayers())
            player.sendMessage(msg);
    }

    public static void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(Display.format(title), Display.format(subtitle));
    }

    public static void clearTitle(Player player) {
        player.sendTitle("", "");
    }

    public static void sendPopupText(Player player, String message, Object ... args) {
        String msg = format(message, args);
        CraftPlayer craftPlayer = (CraftPlayer) player;
        String json = "{\"text\":\"" + org.json.simple.JSONObject.escape(msg) + "\"}";
        IChatBaseComponent chatBaseComponent = IChatBaseComponent.ChatSerializer.a(json);
        PacketPlayOutChat packetPlayOutChat = new PacketPlayOutChat(chatBaseComponent, (byte) 2);
        craftPlayer.getHandle().playerConnection.sendPacket(packetPlayOutChat);
    }
}
