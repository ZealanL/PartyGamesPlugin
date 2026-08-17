package zealan.pgp.game.games;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BlockRange;
import zealan.pgp.util.EntityUtil;
import zealan.pgp.util.PlayerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static zealan.pgp.Globals.PLUGIN;

public class GameBoatRace extends Game {
    public GameBoatRace(InitParams params) {
        super(params);
    }

    private static class LogScroller {
        final int x, y, minZ, maxZ, scrollInterval, scrollDir;

        LogScroller(int x, int y, int z1, int z2, int scrollInterval, int scrollDir) {
            this.x = x;
            this.y = y;
            this.minZ = Math.min(z1, z2);
            this.maxZ = Math.max(z1, z2);
            this.scrollInterval = scrollInterval;
            this.scrollDir = scrollDir;
        }
    }

    private static final LogScroller[] LOG_SCROLLERS = {
            new LogScroller(2474, 127, -2436, -2478, 20, 1),
            new LogScroller(2490, 119, -2442, -2475, 10, -1),
            new LogScroller(2514, 107, -2448, -2466, 20, 1),
            new LogScroller(2534, 97, -2479, -2437, 10, -1),
            new LogScroller(2556, 86, -2478, -2438, 20, 1),
            new LogScroller(2576, 76, -2480, -2436, 10, -1),
            new LogScroller(2613, 58, -2438, -2478, 20, 1),
            new LogScroller(2636, 46, -2478, -2438, 20, -1),
            new LogScroller(2648, 40, -2478, -2438, 20, 1),
            new LogScroller(2660, 34, -2478, -2438, 10, -1),
    };

    private static final BlockRange GLASS_WALL = BlockRange.fromCorners(
            2452, 140, -2477,
            2452, 138, -2438
    );

    private static final int FINISH_LINE_X = 2665;

    @Override
    public void spawnGamer(Gamer gamer) {
        super.spawnGamer(gamer);
        var location = gamer.player.getLocation();
        var boat = (Boat) world.spawnEntity(gamer.player.getLocation(), EntityType.BOAT);

        PlayerUtil.syncMount(gamer.player, boat);
        EntityUtil.setYaw(boat, 90);
        EntityUtil.setPosOnly(boat, location.toVector());
    }

    @Override
    protected void innerOnStart() {
        for (var blockPos : GLASS_WALL) {
            world.getBlockAt(blockPos.x, blockPos.y, blockPos.z).setType(Material.AIR);
        }
    }

    @Override
    protected void innerOnTick() {
        if (hasStarted()) {
            for (var scroller : LOG_SCROLLERS) {
                if (getTicksElapsed() % scroller.scrollInterval != 0)
                    continue;

                var blockMaterials = new ArrayList<Material>();
                var blockDatas = new ArrayList<Byte>();
                for (int z = scroller.minZ; z <= scroller.maxZ; z++) {
                    var block = world.getBlockAt(scroller.x, scroller.y, z);
                    blockMaterials.add(block.getType());
                    blockDatas.add(block.getData());
                }

                Collections.rotate(blockDatas, scroller.scrollDir);
                Collections.rotate(blockMaterials, scroller.scrollDir);

                for (int z = scroller.minZ, i = 0; z <= scroller.maxZ; z++, i++) {
                    var block = world.getBlockAt(scroller.x, scroller.y, z);
                    block.setType(blockMaterials.get(i));
                    block.setData(blockDatas.get(i));
                }
            }

            for (var gamer : getPlayingGamers()) {
                var boat = gamer.player.getVehicle();
                if (boat != null) {
                    if (boat.getLocation().getX() > FINISH_LINE_X) {
                        gamer.stopPlaying(true);
                        gamer.player.playSound(gamer.player.getLocation(), Sound.ORB_PICKUP, 0.75f, 1.0f);
                    }
                } else if (gamer.player.getGameMode() != GameMode.CREATIVE) {
                    // Boat broke
                    world.playSound(gamer.player.getLocation(), Sound.ITEM_BREAK, 0.75f, 0.75f);
                    spawnGamer(gamer);
                    gamer.player.playSound(gamer.player.getLocation(), Sound.ITEM_BREAK, 0.75f, 0.75f);
                }
            }
        }

        for (var gamer : getPlayingGamers()) {
            var boat = gamer.player.getVehicle();
            if (boat != null)
                PlayerUtil.syncMount(gamer.player, boat);
        }
    }

    @Override
    protected void innerOnEnd() {

    }
}
