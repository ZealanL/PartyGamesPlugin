package zealan.pgp.game.games;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BlockRange;
import zealan.pgp.math.Vec3i;

import java.util.ArrayList;

public class GameLabEscape extends Game {

    private static final int BLOCKS_TOP_Y = 160;
    private static final int BLOCKS_BOTTOM_Y = 91;
    private static final int ESCAPED_Y_THRESH = 82;

    private static final Material[] BLOCK_TYPES = {
            Material.WOOD, Material.DIRT, Material.STONE
    };

    private final ArrayList<BlockRange> blocksRanges = new ArrayList<>();

    public GameLabEscape(InitParams params) {
        super(params);

        for (var spawnPoint : config.loadInfo.spawns) {
            Vec3i spawnPos = spawnPoint.getBlockPos();
            BlockRange range = BlockRange.fromCorners(
                    spawnPos.x, BLOCKS_TOP_Y, spawnPos.z,
                    spawnPos.x, BLOCKS_BOTTOM_Y, spawnPos.z
            );
            blocksRanges.add(range);
        }
    }

    @Override
    public void onLoaded() {
        super.onLoaded();

        ArrayList<Material> blockTypes = new ArrayList<>();
        for (int y = BLOCKS_BOTTOM_Y; y <= BLOCKS_TOP_Y; y++) {
            blockTypes.add(BLOCK_TYPES[rand.nextInt(BLOCK_TYPES.length)]);
        }

        for (BlockRange blocksRange : blocksRanges) {
            int i = 0;
            for (Vec3i blockPos : blocksRange) {
                Block block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
                block.setType(blockTypes.get(i));
                i++;
            }
        }

        for (Gamer gamer : getPlayingGamers()) {
            gamer.player.getInventory().setItem(0, new ItemStack(Material.IRON_SPADE));
            gamer.player.getInventory().setItem(1, new ItemStack(Material.IRON_PICKAXE));
            gamer.player.getInventory().setItem(2, new ItemStack(Material.IRON_AXE));
        }
    }

    @Override
    protected void innerOnStart() {}

    @Override
    protected void innerOnEnd() {
    }

    @Override
    protected void innerOnTick() {
        for (Gamer gamer : getPlayingGamers()) {
            if (gamer.player.getLocation().getY() <= ESCAPED_Y_THRESH) {
                gamer.stopPlaying(true);
                Display.sendMsg(world, "&f{} &aescaped!", gamer.player);
            }
        }
    }

    @Override
    public boolean canBreakBlock(Player player, Block block) {
        if (!hasStarted())
            return false;

        boolean isInRange = false;
        for (var blocksRange : blocksRanges)
            if (blocksRange.contains(Vec3i.from(block.getLocation().toVector())))
                isInRange = true;
        if (!isInRange)
            return false;

        var blockType = block.getType();
        var heldType = player.getItemInHand().getType();
        return switch (heldType) {
            case IRON_AXE -> blockType == Material.WOOD;
            case IRON_SPADE -> blockType == Material.DIRT;
            case IRON_PICKAXE -> blockType == Material.STONE;
            default -> false;
        };
    }

    @Override
    public boolean canUseItem(Player player, ItemStack item) {
        return true;
    }
}
