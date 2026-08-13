package zealan.pgp.party;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import zealan.pgp.AutoListener;
import zealan.pgp.api.command.CommandArg;
import zealan.pgp.api.command.CommandNode;
import zealan.pgp.api.command.CommandResult;
import zealan.pgp.api.display.Display;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static zealan.pgp.Globals.COMMAND_SYS;

public class PartyMgr extends AutoListener {
    private final List<Party> parties = new LinkedList<>();
    private final HashMap<Player, Party> partyMap = new HashMap<>();

    public final int MAX_INVITE_SECS = 60;

    public class PartyInvite {
        public final Player from;
        public final Player to;
        public final Instant sentTime;
        public PartyInvite(Player from, Player to, Instant sentTime) {
            this.from = from;
            this.to = to;
            this.sentTime = sentTime;
        }

        public int secsSince() {
            return (int) Duration.between(sentTime, Instant.now()).getSeconds();
        }
    }

    public final LinkedList<PartyInvite> invites = new LinkedList<>();

    public boolean isInParty(Player player) {
        return partyMap.containsKey(player);
    }

    public Party getParty(Player player) {
        return partyMap.get(player);
    }

    public Party createParty(Player player) {
        assert !isInParty(player);
        Party party = new Party(player);
        parties.add(party);
        partyMap.put(player, party);
        return party;
    }

    public void leaveParty(Player player) {
        assert isInParty(player);
        invites.removeIf(invite -> invite.from.equals(player));

        Party party = getParty(player);
        Player leader = party.getLeader();
        if (leader.equals(player)) {
            if (party.getMembers().size() > 1)
                sendMsg(party, "&c&o{}'s party has been disbanded!", player);
            parties.remove(party);
            for (Player member : party.getMembers())
                partyMap.remove(member);
        } else {
            sendMsg(player, "&cYou left {}'s party.", leader);
            party.removeMember(player);
            partyMap.remove(player);
            sendMsg(party, "&c{} left the party.", player);
        }
    }

    public void joinParty(Player player, Party party) {
        assert !isInParty(player);

        sendMsg(party, "&a{} joined the party!", player);
        sendMsg(player, "&aYou joined {}'s party!", party.getLeader());
        partyMap.put(player, party);
        party.addMember(player);
    }

    public void invitePlayer(Player from, Player to) {
        assert invites.stream().noneMatch(invite -> invite.from.equals(from) && invite.to.equals(to));
        assert from != to;
        Party party = getParty(from);
        assert party != null;
        invites.add(new PartyInvite(from, to, Instant.now()));
        sendMsg(party, "&6{} invited {} to your party, they have {} seconds to accept...", from, to, MAX_INVITE_SECS);

        String baseMsg;
        if (party.getLeader().equals(from)) {
            baseMsg = Display.format("&6{} invited you to join their party!", from);
        } else {
            baseMsg = Display.format("&6{} invited you to join {}'s party!", party.getLeader());
        }
        baseMsg += " &n&9[CLICK TO ACCEPT]";

        Display.sendCommandMsg(
                to, "party accept " + from.getName(), "Click to accept the invite", baseMsg
        );
    }

    public void kickPlayer(Party party, Player player) {
        assert !party.getLeader().equals(player);

        sendMsg(player, "&cYou were kicked from the party.");
        party.removeMember(player);
        invites.removeIf(invite -> invite.from.equals(player));
        partyMap.remove(player);
        sendMsg(party, "&c{} was kicked from the party.", player);
    }

    // /////////////////

    @Override
    public void onTick() {
        invites.removeIf(invite ->
                invite.secsSince() > MAX_INVITE_SECS
        );
    }


    @EventHandler
    protected void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isInParty(player))
            leaveParty(player);

        invites.removeIf(invite ->
                invite.from.equals(player) || invite.to.equals(player)
        );
    }

    private static void sendMsg(Player player, String msg, Object... args) {
        Display.sendMsg(player, "&d&lParties&7: " + msg, args);
    }

    private static void sendMsg(Party party, String msg, Object... args) {
        for (Player member : party.getMembers())
            sendMsg(member, msg, args);
    }

    private static void sendErrorMsg(Player player, String msg, Object... args) {
        Display.sendMsg(player, "&d&lParties&7: &c" + msg, args);
    }

    // /////////////////

    private CommandResult cmdQuery(Player player) {
        Party party = getParty(player);
        if (party == null)
            return CommandResult.failure("You are not in a party.");

        StringBuilder msg = new StringBuilder("&dYour party consists of:");
        for (Player member : party.getMembers()) {
            msg.append("\n&7 - ");
            if (party.getLeader().equals(member))
                msg.append("&e♛ ");
            msg.append("&f" + member.getName());

            if (member == player)
                msg.append(" &7&o(you)");
        }

        sendMsg(player, msg.toString());
        return CommandResult.ok();
    }

    private CommandResult cmdInvite(Player player, Player target) {
        if (player == target)
            return CommandResult.failure("You cannot invite yourself! Good try though.");

        Party party = getParty(player);
        if (party == null)
            party = createParty(player);

        if (party.hasMember(target))
            return CommandResult.failure("This player is already in your party!");

        if (invites.stream().anyMatch(invite -> invite.from.equals(player) && invite.to.equals(target)))
            return CommandResult.failure("You already invited that player!");

        invitePlayer(player, target);
        return CommandResult.ok();
    }

    private CommandResult cmdLeave(Player player) {
        Party party = getParty(player);
        if (party == null)
            return CommandResult.failure("You are not in a party!");

        if (party.getLeader().equals(player)) {
            return CommandResult.failure(
                    "You are the party leader! To disband your party, do: {}", "&f/party disband"
            );
        }

        leaveParty(player);
        return CommandResult.ok();
    }

    private CommandResult cmdDisband(Player player) {
        Party party = getParty(player);
        if (party == null)
            sendErrorMsg(player, "You are not in a party!");

        if (!party.getLeader().equals(player))
            return CommandResult.failure("You are not the party leader!");

        leaveParty(player);
        return CommandResult.ok();
    }

    private CommandResult cmdAccept(Player player, Player target) {
        PartyInvite invite = invites.stream()
                .filter(iv -> iv.from == target && iv.to == player)
                .findAny().orElse(null);

        if (invite == null)
            return CommandResult.failure("You have no invite from this player.");

        Party targetParty = getParty(target);
        assert targetParty != null;
        if (isInParty(player)) {
            if (getParty(player) != targetParty) {
                leaveParty(player);
            } else {
                return CommandResult.failure("You are already in this party!");
            }
        }

        joinParty(player, targetParty);
        invites.remove(invite);
        return CommandResult.ok();
    }

    private CommandResult cmdKick(Player player, Player target) {
        Party party = getParty(player);
        if (party == null) {
            return CommandResult.failure("You are not in a party!");
        }
        if (!party.getLeader().equals(player)) {
            return CommandResult.failure("You are not the party leader!");
        }
        if (!party.hasMember(target)) {
            return CommandResult.failure("That player is not in your party!");
        }

        kickPlayer(party, target);
        return CommandResult.ok();
    }

    public PartyMgr() {
        COMMAND_SYS.register(
                CommandNode.make("p", "Create or join parties with others to play together",
                        ctx -> cmdQuery(ctx.sender)
                ).withAlias("party").withChild(
                        CommandNode.make("invite", "Invite a player to your party",
                                ctx -> cmdInvite(ctx.sender, ctx.getArg("player")),
                                new CommandArg.PlayerArg("player")
                        )
                ).withChild(
                        CommandNode.make("accept", "Accept an invite from a player",
                                ctx -> cmdAccept(ctx.sender, ctx.getArg("player")),
                                new CommandArg.PlayerArg("player")
                        )
                ).withChild(
                        CommandNode.make("leave", "Leave your party",
                                ctx -> cmdLeave(ctx.sender)
                        )
                ).withChild(
                        CommandNode.make("disband", "Disband your party",
                                ctx -> cmdDisband(ctx.sender)
                        )
                ).withChild(
                        CommandNode.make("kick", "Kick a player from your party",
                                ctx -> cmdKick(ctx.sender, ctx.getArg("player")),
                                new CommandArg.PlayerArg("player")
                        )
                )
        );
    }
}
