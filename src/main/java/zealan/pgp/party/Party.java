package zealan.pgp.party;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Party {
    private final Player leader;
    private final List<Player> members;

    public Party(Player leader) {
        this.leader = leader;
        this.members = new ArrayList<>();
        this.members.add(leader);
    }

    public Player getLeader() {
        return leader;
    }

    public List<Player> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public boolean hasMember(Player player) {
        return members.contains(player);
    }

    public void addMember(Player player) {
        if (hasMember(player))
            throw new IllegalArgumentException("Player already is a member");
        members.add(player);
    }

    public void removeMember(Player player) {
        if (!hasMember(player))
            throw new IllegalArgumentException("Player is not a member");
        if (leader.equals(player))
            throw new IllegalArgumentException("Cannot remove leader from party, disband instead");

        members.remove(player);
    }
}
