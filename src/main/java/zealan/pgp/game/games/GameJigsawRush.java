package zealan.pgp.game.games;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BlockRange;
import zealan.pgp.math.Vec3i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class GameJigsawRush extends Game {

    public static final GameVariant VARIANT_MULTI = new GameVariant(
            "multi", "Multi-Play", "Play 10 boards instead of just 1",
            Material.DIODE
    );
    public static final GameVariant VARIANT_BLIND = new GameVariant(
            "blind", "Blind", "The reference board disappears after your first place",
            Material.EYE_OF_ENDER
    );

    public GameJigsawRush(InitParams params) {
        super(params);
    }

    private static final int CENTER_X = 247;
    private static final int CENTER_Z = 1816;

    private static final BlockRange[] DISPLAY_BOARD_RANGES = {
            BlockRange.fromCorners(
                    226, 6, 1812,
                    226, 14, 1820
            ),
            BlockRange.fromCorners(
                    268, 6, 1812,
                    268, 14, 1820
            )
    };

    private static final Material[] BLOCK_PALETTE = {
            Material.DIRT, Material.STONE, Material.COBBLESTONE,
            Material.LOG, Material.WOOD, Material.BRICK,
            Material.GOLD_BLOCK, Material.NETHERRACK, Material.ENDER_STONE
    };

    private Material[] curTargetBoard = null;
    private int resetTickTimer = 0;
    private final HashMap<Gamer, boolean[]> playerBlocksCorrectMap = new HashMap<>();
    private final HashMap<Gamer, Integer> playerPlaceCooldowns = new HashMap<>();

    @Override
    public void spawnGamer(Gamer gamer) {
        super.spawnGamer(gamer);
    }

    private Vec3i[] getPlayerBoardBlocks(Gamer gamer) {
        var spawn = getGamerSpawn(gamer);
        var spawnPos = getGamerSpawn(gamer).getBlockPos();
        boolean sideBool = spawnPos.z < CENTER_Z;

        var boardCenter = spawnPos.alongBlockFace(spawn.getDir(), 5).add(0, 2, 0);

        int i = 0;
        var results = new Vec3i[9];
        int rightDirX = sideBool ? 1 : -1;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                var boardPos = boardCenter.add(dx * rightDirX, dy, 0);
                results[i] = boardPos;
                i++;
            }
        }
        assert i == 9;
        return results;
    }

    private void hideTargetBoardForPlayer(Player player) {
        for (var displayBoardRange : DISPLAY_BOARD_RANGES) {
            for (var blockPos : displayBoardRange) {
                player.sendBlockChange(
                        new Location(world, blockPos.x, blockPos.y, blockPos.z),
                        Material.SNOW_BLOCK, (byte) 0
                );
            }
        }
    }

    private void generateTargetBoard() {
        var newTargetBoard = new ArrayList<>(Arrays.asList(BLOCK_PALETTE));
        Collections.shuffle(newTargetBoard, rand);
        curTargetBoard = newTargetBoard.toArray(Material[]::new);

        // Set displays
        for (var displayBoardRange : DISPLAY_BOARD_RANGES) {
            var minBlockPos = displayBoardRange.min();
            for (var blockPos : displayBoardRange) {
                Vec3i offset = blockPos.subtract(minBlockPos);
                var slotOffsetX = offset.z / 3;
                var slotOffsetY = offset.y / 3;
                if (blockPos.x < CENTER_X)
                    slotOffsetX = 2 - slotOffsetX;

                var index = slotOffsetX * 3 + slotOffsetY;
                Block b = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
                b.setType(curTargetBoard[index]);
            }
        }
    }

    private void resetBoard() {
        generateTargetBoard();
        for (var gamer : getPlayingGamers()) {
            playerBlocksCorrectMap.put(gamer, new boolean[9]);

            for (int i = 0; i < 9; i++) {
                var itemStack = new ItemStack(BLOCK_PALETTE[i], 1);
                gamer.player.getInventory().setItem(i, itemStack);
            }

            var boardBlocks = getPlayerBoardBlocks(gamer);
            for (var boardBlock : boardBlocks) {
                Block b = world.getBlockAt(boardBlock.x, boardBlock.y, boardBlock.z);
                if (b.getType() != Material.SNOW_BLOCK) {
                    b.setType(Material.AIR);
                }
                b.setType(Material.SNOW_BLOCK);
            }
        }
    }

    @Override
    protected void innerOnStart() {
        resetBoard();
    }

    @Override
    protected void innerOnEnd() {

    }

    @Override
    protected void innerOnTick() {
        if (resetTickTimer > 0) {
            resetTickTimer--;
            if (resetTickTimer == 0)
                resetBoard();
        }

        playerPlaceCooldowns.clear();
    }

    @Override
    public boolean canInteractBlock(Player player, Block block) {
        if (!hasStarted() || curTargetBoard == null)
            return true;

        var blockPos = new Vec3i(block.getX(), block.getY(), block.getZ());

        var gamer = getGamer(player);
        if (gamer != null && gamer.isPlaying()) {
            var boardBlocks = getPlayerBoardBlocks(gamer);
            for (int i = 0; i < 9; i++) {
                if (blockPos.equals(boardBlocks[i])) {
                    ItemStack mainHandStack = gamer.player.getItemInHand();
                    if (mainHandStack != null && mainHandStack.getType() != Material.AIR) {
                        Material placedMaterial = mainHandStack.getType();
                        if (placedMaterial.isBlock()) {
                            Block b = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
                            Material existingMaterial = b.getType();
                            if (placedMaterial == existingMaterial)
                                return true;

                            int selectedSlot = gamer.player.getInventory().getHeldItemSlot();
                            if (playerPlaceCooldowns.containsKey(gamer))
                                if (playerPlaceCooldowns.get(gamer) == selectedSlot)
                                    return true;

                            playerPlaceCooldowns.put(gamer, selectedSlot);

                            b.setType(placedMaterial);

                            boolean[] correctMap = playerBlocksCorrectMap.get(gamer);
                            boolean correctPlacement = placedMaterial == curTargetBoard[i];
                            correctMap[i] = correctPlacement;

                            gamer.player.playSound(gamer.player.getLocation(), Sound.DIG_SNOW, 1.0f, 1.0f);

                            if (correctPlacement) {
                                int numPlacementsCorrect = 0;
                                for (boolean val : correctMap)
                                    if (val)
                                        numPlacementsCorrect++;

                                final double BASE_PITCH = 0.75;
                                final int[] SCALE_NOTES = {
                                        -3, 0, 2, 4, 5, 7, 9, 11, 12
                                };

                                int note = SCALE_NOTES[numPlacementsCorrect - 1];
                                double pitch = BASE_PITCH * calcNotePitch(note);
                                gamer.player.playSound(gamer.player.getLocation(), Sound.ORB_PICKUP, 0.7f, (float) pitch);

                                if (numPlacementsCorrect == 1 && variant == VARIANT_BLIND)
                                    hideTargetBoardForPlayer(gamer.player);

                                if (numPlacementsCorrect >= 9) {
                                    gamer.player.playSound(gamer.player.getLocation(), Sound.LEVEL_UP, 0.7f, (float) (BASE_PITCH * 2.0));

                                    for (var otherGamer : getGamers()) {
                                        Display.sendMsg(
                                                otherGamer.player,
                                                "&a" + player.getName() + " finished their board!"
                                        );
                                    }

                                    if (variant == VARIANT_MULTI) {
                                        gamer.points++;
                                        if (gamer.points == 10) {
                                            gamer.stopPlaying(true);
                                        } else {
                                            resetTickTimer = 20;
                                        }
                                    } else {
                                        gamer.stopPlaying(true);
                                    }
                                }
                            }
                        }
                    }

                    return true;
                }
            }
        }

        return true;
    }

    // TODO: Move
    private double calcNotePitch(int note) {
        return Math.pow(2.0, note / 12.0);
    }

    @Override
    public boolean canUseItem(Player player, ItemStack stack) {
        return true;
    }
}