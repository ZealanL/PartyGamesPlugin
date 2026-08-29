package zealan.pgp.game.games;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.PlayerUtil;

import java.util.Arrays;

public class GameHoeHoeHoe extends Game {
    private static final int GRASS_Y = 111;
    private static final int SPAWN_Y = GRASS_Y + 1;

    private static final DyeColor[] TEAM_DYES = {
            DyeColor.WHITE,
            DyeColor.RED,
            DyeColor.PINK,
            DyeColor.LIME,
            DyeColor.BLACK,
            DyeColor.BLUE,
            DyeColor.PURPLE,
            DyeColor.ORANGE
    };

    private static final Vec3i[] START_SPAWNS = new Vec3i[] {
            new Vec3i(1162, SPAWN_Y, -1862),
            new Vec3i(1162, SPAWN_Y, -1873),
            new Vec3i(1163, SPAWN_Y, -1864),
            new Vec3i(1163, SPAWN_Y, -1869),
            new Vec3i(1164, SPAWN_Y, -1864),
            new Vec3i(1164, SPAWN_Y, -1875),
            new Vec3i(1165, SPAWN_Y, -1869),
            new Vec3i(1167, SPAWN_Y, -1876),
            new Vec3i(1168, SPAWN_Y, -1863),
            new Vec3i(1170, SPAWN_Y, -1876),
            new Vec3i(1171, SPAWN_Y, -1865),
            new Vec3i(1171, SPAWN_Y, -1873),
            new Vec3i(1173, SPAWN_Y, -1874),
            new Vec3i(1174, SPAWN_Y, -1870),
            new Vec3i(1174, SPAWN_Y, -1872),
            new Vec3i(1177, SPAWN_Y, -1862),
            new Vec3i(1177, SPAWN_Y, -1874)
    };

    public GameHoeHoeHoe(InitParams params) {
        super(params);
    }

    @Override
    protected void innerOnStart() {
        for (var gamer : getPlayingGamers()) {
            var pos = START_SPAWNS[rand.nextInt(START_SPAWNS.length)];
            gamer.player.teleport(new Location(world, pos.x, pos.y, pos.z));
            gamer.player.getInventory().setItem(0, new ItemStack(Material.DIAMOND_HOE));
        }
    }

    @Override
    public boolean canUseItem(Player player, ItemStack item) {
        return item.getType() == Material.DIAMOND_HOE;
    }

    @Override
    public boolean canInteractBlock(Player player, Block block) {
        if (!hasStarted())
            return false;

        var gamer = getPlayingGamer(player);
        if (gamer == null)
            return false;

        if (player.getItemInHand().getType() != Material.DIAMOND_HOE)
            return false;

        if (block.getY() != GRASS_Y || (block.getType() != Material.GRASS && block.getType() != Material.MYCEL))
            return false;

        var playerDye = TEAM_DYES[gamer.spawnIdx % TEAM_DYES.length];

        Vec3i blockPos = new Vec3i(block.getX(), block.getY(), block.getZ());
        if (gamer.points > 0) {
            // Require neighboring block
            var neighborings = new Vec3i[] {
                    blockPos.add(-1, 0, 0),
                    blockPos.add(1, 0, 0),
                    blockPos.add(0, 0, -1),
                    blockPos.add(0, 0, 1),
            };
            boolean hasNeighbor = Arrays.stream(neighborings).anyMatch(
                    pos -> {
                        var otherBlock = world.getBlockAt(pos.x, pos.y, pos.z);
                        if (otherBlock.getType() != Material.WOOL)
                            return false;
                        Wool woolData = (Wool) otherBlock.getState().getData();
                        return woolData.getColor() == playerDye;
                    }
            );

            if (!hasNeighbor) {
                Display.sendPopupText(player, "&cYou can only hoe next to your own wool!");
                return false;
            }
        }

        block.setType(Material.WOOL);
        block.setData(playerDye.getWoolData());
        gamer.points++;
        world.playSound(block.getLocation(), Sound.LAVA_POP, 1.0f, 0.5f);
        PlayerUtil.makeSwingHand(player);
        var particleLoc = blockPos.up().getBottomCenter();
        world.spigot().playEffect(
                new Location(world, particleLoc.getX(), particleLoc.getY(), particleLoc.getZ()), Effect.MAGIC_CRIT,
                0, 1,
                0.3f, 0.3f, 0.3f,
                0.2f, 7, 64
        );
        return false;
    }

    @Override
    protected void innerOnTick() {

    }

    @Override
    protected void innerOnEnd() {

    }
}
