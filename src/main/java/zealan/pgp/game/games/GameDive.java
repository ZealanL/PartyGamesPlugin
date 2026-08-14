package zealan.pgp.game.games;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameSpawn;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;

public class GameDive extends Game {
    public GameDive(InitParams params) {
        super(params);
    }

    public static final int SPAWN_Y = 78;
    public static final int WATER_Y = 14;
    public static final int MISSED_THRESHOLD_Y = 0;

    public static final byte[] TEAM_WOOL_DATA = {
            11, // BLUE
            2,  // MAGENTA
            15, // BLACK
            1,  // ORANGE
            4,  // YELLOW
            14, // RED
            5,  // LIME
            0   // WHITE
    };

    @Override
    protected void innerOnStart() {

    }

    @Override
    protected void innerOnEnd() {

    }

    @Override
    protected void innerOnTick() {
        if (!hasStarted()) {
            for (var gamer : getPlayingGamers()) {
                Player player = gamer.player;
                var spawn = getGamerSpawn(gamer);
                var spawnBlock = Vec3i.from(spawn.pos);
                var playerBlockPos = Vec3i.from(player.getLocation().toVector());

                if (Math.abs(spawnBlock.x - playerBlockPos.x) > 1 || Math.abs(spawnBlock.z - playerBlockPos.z) > 1)
                    EntityUtil.setPosOnly(gamer.player, spawn.pos);
            }
        } else {

            for (var gamer : getPlayingGamers()) {
                Player player = gamer.player;
                if (player == null || !player.isOnline()) continue;

                // They fell and missed the platform entirely...
                if (player.getLocation().getY() < MISSED_THRESHOLD_Y) {
                    spawnGamer(gamer);
                    continue;
                }

                var loc = player.getLocation();
                var y = loc.getY();
                if (y < WATER_Y + 10) {
                    var spawn = getGamerSpawn(gamer);

                    Block block = loc.getBlock();
                    if (block.getType() == Material.STATIONARY_WATER) {
                        EntityUtil.setPosOnly(gamer.player, spawn.pos);

                        block.setType(Material.WOOL);
                        block.setData(TEAM_WOOL_DATA[gamer.spawnIdx]);
                        gamer.points++;
                        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.8f, 1.0f);
                        Display.sendPopupText(player, "&a+1");
                    } else if (player.isOnGround() && y > WATER_Y) {
                        EntityUtil.setPosOnly(gamer.player, spawn.pos);

                        Display.sendPopupText(player, "&cMiss!");
                    }
                }
            }
        }
    }
}