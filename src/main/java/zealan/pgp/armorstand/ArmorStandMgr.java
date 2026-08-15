package zealan.pgp.armorstand;

import org.bukkit.Location;
import zealan.pgp.AutoListener;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.WorldUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArmorStandMgr extends AutoListener {
    private static class ArmorStandStack {
        final Location topLoc;
        final ArrayList<FakeArmorStand> stands = new ArrayList<>();

        static final double OFFSET_Y = -0.4;

        ArmorStandStack(Location topLoc) {
            this.topLoc = topLoc;
        }

        void setLines(List<String> lines) {
            while (stands.size() < lines.size())
                stands.add(new FakeArmorStand(topLoc.clone().add(0, stands.size() * OFFSET_Y, 0)));

            for (int i = 0; i < stands.size(); i++) {
                if (lines.size() > i) {
                    stands.get(i).setName(lines.get(i));
                } else {
                    stands.get(i).clearName();
                }
            }
        }

        void updateToPlayers() {
            for (var stand : stands)
                stand.updateToPlayers();
        }
    }

    private final HashMap<Vec3i, ArmorStandStack> stacks = new HashMap<>();

    private final static int UPDATE_INTERVAL_TICKS = 5;

    public ArmorStandMgr() {}

    @Override
    public void onTick() {
        if (getListenerTickCount() % UPDATE_INTERVAL_TICKS == 0) {
            for (var stack : stacks.values())
                stack.updateToPlayers();
        }
    }

    public void setAtPos(Vec3i pos, List<String> lines) {
        if (!stacks.containsKey(pos))
            stacks.put(pos, new ArmorStandStack(new Location(WorldUtil.getMainWorld(), pos.x, pos.y, pos.z)));

        var stack = stacks.get(pos);
        stack.setLines(lines);
    }
}
