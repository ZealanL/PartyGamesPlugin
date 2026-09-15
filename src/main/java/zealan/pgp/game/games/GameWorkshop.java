package zealan.pgp.game.games;

import com.github.retrooper.packetevents.protocol.world.Direction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameSpawn;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BlockRange;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;
import zealan.pgp.util.ItemUtil;
import zealan.pgp.util.WorldUtil;

import java.util.*;

import static zealan.pgp.Globals.PLOG;
import static zealan.pgp.Globals.PLUGIN;

public class GameWorkshop extends Game {
    public static final GameSpawn[] SPAWNS = new GameSpawn[] {
            new GameSpawn(-1845+(21*0), 39, 719, BlockFace.NORTH),
            new GameSpawn(-1845+(21*1), 39, 719, BlockFace.NORTH),
            new GameSpawn(-1845+(21*2), 39, 719, BlockFace.NORTH),
            new GameSpawn(-1845+(21*3), 39, 719, BlockFace.NORTH),
            new GameSpawn(-1845+(21*4), 39, 719, BlockFace.NORTH),
            new GameSpawn(-1845+(21*5), 39, 719, BlockFace.NORTH),
            new GameSpawn(-1845+(21*6), 39, 719, BlockFace.NORTH),
            new GameSpawn(-1845+(21*7), 39, 719, BlockFace.NORTH),

            new GameSpawn(-1853+(21*0), 39, 759, BlockFace.SOUTH),
            new GameSpawn(-1853+(21*1), 39, 759, BlockFace.SOUTH),
            new GameSpawn(-1853+(21*2), 39, 759, BlockFace.SOUTH),
            new GameSpawn(-1853+(21*3), 39, 759, BlockFace.SOUTH),
            new GameSpawn(-1853+(21*4), 39, 759, BlockFace.SOUTH),
            new GameSpawn(-1853+(21*5), 39, 759, BlockFace.SOUTH),
            new GameSpawn(-1853+(21*6), 39, 759, BlockFace.SOUTH),
            new GameSpawn(-1853+(21*7), 39, 759, BlockFace.SOUTH)
    };

    public static final Material[] CRAFTS = new Material[] {
            Material.WOOD_BUTTON,
            Material.WOOD_PLATE,
            Material.WORKBENCH,
            Material.BOAT,
            Material.CHEST,
            Material.FENCE,
            Material.FENCE_GATE,
            Material.SIGN,

            Material.WOOD_SWORD,
            Material.WOOD_PICKAXE,
            Material.WOOD_AXE,
            Material.WOOD_HOE,

            Material.STONE_SWORD,
            Material.STONE_PICKAXE,
            Material.STONE_AXE,
            Material.STONE_HOE,

            Material.IRON_SWORD,
            Material.IRON_PICKAXE,
            Material.IRON_AXE,
            Material.IRON_HOE,

            Material.GOLD_SWORD,
            Material.GOLD_PICKAXE,
            Material.GOLD_AXE,
            Material.GOLD_HOE,

            Material.DIAMOND_SWORD,
            Material.DIAMOND_PICKAXE,
            Material.DIAMOND_AXE,
            Material.DIAMOND_HOE,

            Material.IRON_HELMET,
            Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,
            Material.IRON_BOOTS,

            Material.GOLD_HELMET,
            Material.GOLD_CHESTPLATE,
            Material.GOLD_LEGGINGS,
            Material.GOLD_BOOTS,

            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS,

            Material.COAL_BLOCK,
            Material.LAPIS_BLOCK,
            Material.EMERALD_BLOCK,
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.REDSTONE_BLOCK,

            Material.NOTE_BLOCK,
            Material.FURNACE,
            Material.TORCH,
            Material.REDSTONE_TORCH_ON,

            Material.SHEARS,
            Material.MINECART,
            Material.CAULDRON_ITEM,
            Material.COMPASS,
            Material.ARMOR_STAND,
            Material.ENCHANTMENT_TABLE,
            Material.RAILS,
            Material.PISTON_BASE,
            Material.BOW,
            Material.FISHING_ROD
    };

    public static final Material[] MISC_MATERIAL_BLOCKS = new Material[] {
            Material.STONE,
            Material.LOG,
            Material.WEB,
            Material.MELON_BLOCK,

            Material.IRON_ORE,
            Material.GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.DIAMOND_ORE
    };

    private class PlayInfo {
        final Gamer gamer;
        final int spawnIdx;
        final int sidedDir;
        final Vec3i[] blockPositions;
        int craftIdx = 0;
        final Villager villager;
        int progressTimer = 0;
        int craftTicks = 0;

        PlayInfo(Gamer gamer) {
            this.gamer = gamer;
            this.spawnIdx = gamer.spawnIdx;
            this.sidedDir = rand.nextBoolean() ? -1 : 1;
            this.blockPositions = getBlockPositionsFromSponge(spawnIdx);
            this.villager = spawnVillager(spawnIdx, sidedDir);
        }

        boolean isDone() {
            return craftIdx >= crafts.length;
        }

        boolean isProgressing() {
            return progressTimer > 0;
        }
    }

    // ////////////////

    final PlayInfo[] playInfos;
    final Material[] crafts;

    public GameWorkshop(InitParams params) {
        super(params);

        var shuffledCrafts = new ArrayList<>(List.of(CRAFTS.clone()));
        Collections.shuffle(shuffledCrafts, this.rand);
        this.crafts = shuffledCrafts.subList(0, 6).toArray(Material[]::new);

        playInfos = new PlayInfo[SPAWNS.length];
        for (var gamer : getPlayingGamers()) {
            int spawnIdx = gamer.spawnIdx;
            playInfos[spawnIdx] = new PlayInfo(gamer);
        }
    }

    private static List<Material> getIngredientsFor(Material targetMaterial) {
        List<Material> ingredients = new ArrayList<>();
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();

        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            if (recipe.getResult().getType() == targetMaterial) {
                if (recipe instanceof ShapedRecipe shaped) {
                    for (ItemStack item : shaped.getIngredientMap().values()) {
                        if (item != null && item.getType() != Material.AIR) {
                            ingredients.add(item.getType());
                        }
                    }

                    break;
                } else if (recipe instanceof ShapelessRecipe shapeless) {
                    for (ItemStack item : shapeless.getIngredientList()) {
                        if (item != null && item.getType() != Material.AIR) {
                            ingredients.add(item.getType());
                        }
                    }

                    break;
                }
            }
        }

        return ingredients;
    }

    private static void setBlockForIngredientMaterial(Block block, Material material) {
        Material blockType = switch (material) {
            case COBBLESTONE, STONE -> Material.STONE;
            case STICK, WOOD, LOG -> Material.LOG;
            case STRING -> Material.WEB;
            case BOOK -> Material.BOOKSHELF;

            case COAL -> Material.COAL_ORE;
            case IRON_INGOT -> Material.IRON_ORE;
            case GOLD_INGOT -> Material.GOLD_ORE;
            case DIAMOND -> Material.DIAMOND_ORE;
            case REDSTONE -> Material.REDSTONE_ORE;
            case EMERALD -> Material.EMERALD_ORE;
            default -> material;
        };
        block.setType(blockType);
        if (blockType == Material.LOG) {
            block.setData((byte)1); // Set to spruce
        }
    }

    private Villager spawnVillager(int spawnIdx, int sidedDir) {
        var spawn = SPAWNS[spawnIdx];
        Vector villagerSpawnPos = spawn.getBlockPos().alongBlockFace(spawn.dir, 3).getBottomCenter();
        villagerSpawnPos.setX(
                villagerSpawnPos.getX() + sidedDir * 0.5
        );
        var villagerSpawnLoc = new Location(
                world, villagerSpawnPos.getX(), villagerSpawnPos.getY(), villagerSpawnPos.getZ()
        );
        var villager = world.spawn(
                villagerSpawnLoc,
                Villager.class
        );

        double villagerYaw = spawn.dir == BlockFace.NORTH ? 0 : 180;
        EntityUtil.setNoAI(villager, true);
        EntityUtil.setYaw(villager, (float)villagerYaw);
        EntityUtil.setHeadYaw(villager, (float)villagerYaw);
        return villager;
    }

    private void clearMaterialBlocks(int spawnIdx) {
        PlayInfo playInfo = playInfos[spawnIdx];
        for (var blockPos : playInfo.blockPositions) {
            var block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
            block.setType(Material.AIR);
        }
    }

    private void updateMaterialBlocks(int spawnIdx) {
        PlayInfo playInfo = playInfos[spawnIdx];
        if (playInfo != null) {
            for (var blockPos : playInfo.blockPositions) {
                Material blockType = MISC_MATERIAL_BLOCKS[rand.nextInt(MISC_MATERIAL_BLOCKS.length)];
                var block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
                block.setType(blockType);
                if (blockType == Material.LOG) {
                    block.setData((byte)1); // Set to spruce
                }
            }
        } else {
            throw new IllegalArgumentException("Spawn index has no corresponding play info");
        }

        if (hasStarted()) {
            // Actually put in the material blocks for the next craft
            Material nextCraft = (playInfo.craftIdx < crafts.length) ? crafts[playInfo.craftIdx] : null;
            if (nextCraft != null) {
                int minBlockX = playInfo.blockPositions[0].x;
                int maxBlockX = minBlockX;
                for (var blockPos : playInfo.blockPositions) {
                    minBlockX = Math.min(minBlockX, blockPos.x);
                    maxBlockX = Math.max(maxBlockX, blockPos.x);
                }
                int sidedX = (playInfo.sidedDir < 0) ? minBlockX : maxBlockX;

                ArrayList<Vec3i> lowerSlots = new ArrayList<>(Arrays.stream(playInfo.blockPositions).filter(
                        blockPos -> (blockPos.x == sidedX) && (blockPos.y < 45)
                ).toList());
                Collections.shuffle(lowerSlots, rand);
                ArrayList<Vec3i> upperSlots = new ArrayList<>(Arrays.stream(playInfo.blockPositions).filter(
                        blockPos -> (blockPos.x == sidedX) && (blockPos.y > 45)
                ).toList());
                Collections.shuffle(upperSlots, rand);

                if (lowerSlots.isEmpty() || upperSlots.isEmpty()) {
                    throw new IllegalStateException("Missing slots");
                }

                var materialsNeeded = getIngredientsFor(nextCraft);
                for (int i = 0; i < materialsNeeded.size(); i++) {
                    boolean useUpper = rand.nextDouble() < 0.10; // TODO: Guess
                    Vec3i blockPos = useUpper ? upperSlots.get(i) : lowerSlots.get(i);
                    Block slotBlock = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
                    setBlockForIngredientMaterial(slotBlock, materialsNeeded.get(i));
                }
            } else {
                for (var blockPos : playInfo.blockPositions) {
                    world.getBlockAt(blockPos.x, blockPos.y, blockPos.z).setType(Material.AIR);
                }
            }
        }
    }

    private BlockRange getMajorBlockRangeFromSpawn(int spawnIdx) {
        GameSpawn spawn = SPAWNS[spawnIdx];
        int forwardFlip = (spawn.dir == BlockFace.NORTH) ? 1 : -1;
        Vec3i a = spawn.getBlockPos().add(-16 * forwardFlip, 0, -3 * forwardFlip);
        Vec3i b = spawn.getBlockPos().add(4 * forwardFlip, 15, 16 * forwardFlip);
        return BlockRange.fromCorners(a, b);
    }

    private Vec3i[] getBlockPositionsFromSponge(int spawnIdx) {
        var results = new ArrayList<Vec3i>();

        BlockRange range = getMajorBlockRangeFromSpawn(spawnIdx);
        for (Vec3i pos : range) {
            var block = world.getBlockAt(pos.x, pos.y, pos.z);
            if (block.getType() == Material.SPONGE) {
                results.add(pos);
            }
        }

        return results.toArray(Vec3i[]::new);
    }

    private void updateCraft(Gamer gamer) {
        var playInfo = playInfos[gamer.spawnIdx];
        if (!playInfo.isDone()) {
            Material craft = crafts[playInfo.craftIdx];

            gamer.player.playSound(playInfo.villager.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
            updateMaterialBlocks(gamer.spawnIdx);

            Display.sendMsg(gamer.player, "&7Villager: &fCraft me {}!", "&d" + ItemUtil.getMaterialNameLower(craft));
            Display.sendPopupText(gamer.player, "Item: {}", "&d" + ItemUtil.getMaterialNameLower(craft));
            gamer.player.getInventory().clear();
            gamer.player.updateInventory();
        }
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        for (int i = 0; i < SPAWNS.length; i++) {
            var blockRange = getMajorBlockRangeFromSpawn(i);
            for (var blockPos : blockRange) {
                var block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z);
                if (block.getType() == Material.SPONGE) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    @Override
    public void spawnGamer(Gamer gamer) {
        super.spawnGamer(gamer);
        clearMaterialBlocks(gamer.spawnIdx);
    }

    @Override
    protected void innerOnStart() {
        for (var gamer : getPlayingGamers()) {
            updateCraft(gamer);
        }
    }

    @Override
    protected void innerOnTick() {
        if (!hasStarted())
            return;

        // Update progression
        for (var gamer : getPlayingGamers()) {
            var playInfo = playInfos[gamer.spawnIdx];
            if (!playInfo.isDone()) {
                if (playInfo.isProgressing()) {
                    playInfo.progressTimer--;
                    if (playInfo.progressTimer == 0) {
                        playInfo.craftIdx++;
                        playInfo.craftTicks = 0;
                        updateCraft(gamer);
                    }
                } else {
                    playInfo.craftTicks++;
                }
            }
        }

        // Auto-fuel furnaces
        for (var gamer : getPlayingGamers()) {
            var openInv = gamer.player.getOpenInventory();
            if (openInv != null && openInv.getTopInventory() != null) {
                var topInv = openInv.getTopInventory();
                if (topInv instanceof FurnaceInventory furnaceInv) {
                    furnaceInv.setFuel(new ItemStack(Material.COAL, 64));
                }
            }
        }
    }

    private void onGamerMineResourceBlock(Gamer gamer, Block block) {
        Material material = block.getType();
        var blockDrops = block.getDrops();
        if (!blockDrops.isEmpty()) {
            material = blockDrops.iterator().next().getType();
        }

        WorldUtil.breakBlock(world, block);
        gamer.player.getInventory().addItem(new ItemStack(material));
    }

    private ItemStack getSmeltingResult(Material material) {
        Iterator<Recipe> recipeIter = Bukkit.recipeIterator();
        while (recipeIter.hasNext()) {
            Recipe recipe = recipeIter.next();
            if (recipe instanceof FurnaceRecipe furnaceRecipe) {
                if (furnaceRecipe.getInput().getType() == material) {
                    return furnaceRecipe.getResult();
                }
            }
        }
        return null;
    }

    private void updateFurnaceInv(FurnaceInventory furnaceInv) {
        ItemStack smeltingStack = furnaceInv.getSmelting();
        if (smeltingStack == null)
            return;
        var smeltingResult = getSmeltingResult(smeltingStack.getType());
        if (smeltingResult != null) {
            smeltingResult.setAmount(Math.min(smeltingStack.getAmount() * smeltingResult.getAmount(), 64));

            furnaceInv.setSmelting(new ItemStack(Material.AIR));
            var existingResult = furnaceInv.getResult();
            if ((existingResult != null) && (existingResult.getType() != Material.AIR) && (existingResult.getType() == smeltingResult.getType())) {
                existingResult.setAmount(Math.min(existingResult.getAmount() + smeltingResult.getAmount(), 64));
            } else {
                furnaceInv.setResult(smeltingResult);
            }

            for (var viewer : furnaceInv.getViewers()) {
                if (viewer instanceof Player player) {
                    player.updateInventory();
                }
            }
        }
    }

    @Override
    protected void innerOnEnd() {

    }

    @Override
    public boolean canInteractBlock(Player player, Block block) {
        return true;
    }

    @Override
    public boolean canInteractEntity(Player player, Entity entity) {
        var gamer = getPlayingGamer(player);
        if (gamer != null && entity instanceof Villager villager) {
            var playInfo = playInfos[gamer.spawnIdx];
            if (!playInfo.isDone() && !playInfo.isProgressing()) {
                var targetCraft = crafts[playInfo.craftIdx];
                var held = player.getInventory().getItemInHand();
                if (held != null && held.getType() == targetCraft) {
                    Display.sendPopupText(player, "&aComplete! &f[{}/{}]", playInfo.craftIdx + 1, crafts.length);
                    if (playInfo.craftIdx < crafts.length - 1) {
                        player.playSound(villager.getLocation(), Sound.ORB_PICKUP, 1, 1);
                        playInfo.progressTimer = 20;
                    } else {
                        player.playSound(villager.getLocation(), Sound.LEVEL_UP, 0.8f, 1);
                        gamer.stopPlaying(true);
                    }
                    player.getInventory().clear();
                    player.updateInventory();

                    Display.sendMsg(
                            player,
                            "&7You crafted {} in: {}s",
                            "&d" + ItemUtil.getMaterialNameLower(crafts[playInfo.craftIdx]),
                            new Display.TicksTime(playInfo.craftTicks, true)
                    );
                } else if (held != null && held.getType() != Material.AIR) {
                    player.playSound(villager.getLocation(), Sound.NOTE_PLING, 1, 0.5f);
                    Display.sendPopupText(player, "&cWrong item!");
                }
            }
        }

        return false;
    }

    @Override
    public boolean canInteractInv(Player player, Inventory inv, InventoryInteractEvent event) {
        // Block inventory crafting
        var inventory = event.getInventory();
        if (event instanceof InventoryClickEvent clickEvent) {
            inventory = clickEvent.getClickedInventory();
        }
        if (inventory.getType() == InventoryType.CRAFTING) {
            if (inventory.getSize() < 9) {
                return false;
            }
        }

        if (player.getOpenInventory() != null) {
            if (player.getOpenInventory().getTopInventory() instanceof FurnaceInventory furnaceInv) {
                Bukkit.getScheduler().runTaskLater(PLUGIN, () -> this.updateFurnaceInv(furnaceInv), 1);
            }
        }

        return true;
    }

    @Override
    public boolean canUseItem(Player player, ItemStack item) {
        return true;
    }

    @Override
    public boolean canDamageBlock(Player player, Block block) {
        var gamer = getPlayingGamer(player);
        if (gamer != null) {
            var playInfo = playInfos[gamer.spawnIdx];
            var blockPos = new Vec3i(block.getX(), block.getY(), block.getZ());
            for (var slotPos : playInfo.blockPositions) {
                if (slotPos.equals(blockPos)) {
                    onGamerMineResourceBlock(gamer, block);
                    break;
                }
            }
        }
        return true;
    }
}
