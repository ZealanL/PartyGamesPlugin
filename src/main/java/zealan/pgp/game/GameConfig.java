package zealan.pgp.game;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import zealan.pgp.game.games.*;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.ItemUtil;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

public enum GameConfig {
    AnimalSlaughter(
            GameAnimalSlaughter.class, "Animal Slaughter", Material.WOOD_SWORD,
            GameStyle.POINTS, GameRecordStyle.POINTS, GameMode.ADVENTURE, 60,
            new GameLoadInfo(
                    GameAnimalSlaughter.CENTER_BLOCK_POS,
                    new GameSpawn(GameAnimalSlaughter.CENTER_BLOCK_POS)
            )
    ),
    AnvilSpleef(
            GameAnvilSpleef.class, "Anvil Spleef", Material.ANVIL,
            GameStyle.SURVIVAL, GameRecordStyle.LONGEST_TIME, GameMode.ADVENTURE, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new GameSpawn(-243, 1, -3608)
            )
    ),
    /*
    Avalanche(
            GameAvalanche.class, "Avalanche", Items.ICE,
            GameStyle.SURVIVAL, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new BlockPos(-2393, 46, -1878),
                    new GameSpawn[]{new GameSpawn(-2393, 46, -1878)}
            )
    ),
   */
    Bombardment(
            GameBombardment.class, "Bombardment", Material.COAL_BLOCK,
            GameStyle.SURVIVAL, GameRecordStyle.LONGEST_TIME, GameMode.ADVENTURE, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new GameSpawn(-392, 20, 726, BlockFace.EAST)
            )
    ),
    CannonPainting(
            GameCannonPainting.class, "Cannon Painting", Material.EGG,
            GameStyle.POINTS, GameRecordStyle.POINTS, GameMode.ADVENTURE, 30,
            new GameLoadInfo(
                    new GameSpawn(217, 19, 1443)
            )
    ),

    ChickenRings(
            GameChickenRings.class, "Chicken Rings", ItemUtil.getSpawnEgg(EntityType.CHICKEN),
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 120,
            new GameLoadInfo(
                    new Vec3i(2493, 66, -591), // Finish line
                    new GameSpawn(2492, 72, 220, BlockFace.NORTH)
            )
    ),
    Dive(
            GameDive.class, "Dive", Material.WATER_BUCKET,
            GameStyle.POINTS, GameRecordStyle.POINTS, GameMode.ADVENTURE, 60,
            new GameLoadInfo(
                    new GameSpawn(936, 78, -3613),
                    new GameSpawn(939, 78, -3065),
                    new GameSpawn(947, 78, -3602),
                    new GameSpawn(955, 78, -3065),
                    new GameSpawn(958, 78, -3613),
                    new GameSpawn(955, 78, -3621),
                    new GameSpawn(947, 78, -3624),
                    new GameSpawn(939, 78, -3621)
            )
    ),
    HoeHoeHoe(
            GameHoeHoeHoe.class, "Hoe Hoe Hoe", Material.DIAMOND_HOE,
            GameStyle.POINTS, GameRecordStyle.POINTS, GameMode.SURVIVAL, 60,
            new GameLoadInfo(
                    new GameSpawn(1171, 112, -1867)
            )
    ),
    JigsawRush(
            GameJigsawRush.class, "Jigsaw Rush", Material.DAYLIGHT_DETECTOR,
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 60,
            new GameLoadInfo(
                    new Vec3i(247, 2, 1816),
                    new GameSpawn[] {
                            new GameSpawn(262, 2, 1822, BlockFace.SOUTH),
                            new GameSpawn(256, 2, 1822, BlockFace.SOUTH),
                            new GameSpawn(250, 2, 1822, BlockFace.SOUTH),
                            new GameSpawn(244, 2, 1822, BlockFace.SOUTH),
                            new GameSpawn(238, 2, 1822, BlockFace.SOUTH),
                            new GameSpawn(232, 2, 1822, BlockFace.SOUTH),

                            new GameSpawn(262, 2, 1810, BlockFace.NORTH),
                            new GameSpawn(256, 2, 1810, BlockFace.NORTH),
                            new GameSpawn(250, 2, 1810, BlockFace.NORTH),
                            new GameSpawn(244, 2, 1810, BlockFace.NORTH),
                            new GameSpawn(238, 2, 1810, BlockFace.NORTH),
                            new GameSpawn(232, 2, 1810, BlockFace.NORTH)
                    }
            )
    ),
    JungleJump(
            GameJungleJump.class, "Jungle Jump", Material.JUNGLE_WOOD_STAIRS,
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 90,
            new GameLoadInfo(
                    new GameSpawn(-181, 7, 2310, BlockFace.WEST)
            )
    ),
    LabEscape(
            GameLabEscape.class, "Lab Escape", Material.IRON_AXE,
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.SURVIVAL, 180,
            new GameLoadInfo(
                    new GameSpawn(1429, 161, -117),
                    new GameSpawn(1437, 161, -101),
                    new GameSpawn(1437, 161, -125),
                    new GameSpawn(1437, 161, -117),
                    new GameSpawn(1445, 161, -101),
                    new GameSpawn(1453, 161, -109),
                    new GameSpawn(1453, 161, -125),
                    new GameSpawn(1445, 161, -109),
                    new GameSpawn(1453, 161, -101),
                    new GameSpawn(1429, 161, -101),
                    new GameSpawn(1453, 161, -117),
                    new GameSpawn(1445, 161, -117),
                    new GameSpawn(1429, 161, -109),
                    new GameSpawn(1445, 161, -125),
                    new GameSpawn(1437, 161, -109),
                    new GameSpawn(1429, 161, -125)
            )
    ),
    LawnMoower(
            GameLawnMoower.class, "Lawn Moower", new ItemStack(Material.LONG_GRASS, 1, (short)0, (byte)1),
            GameStyle.POINTS, GameRecordStyle.POINTS, GameMode.ADVENTURE, 60,
            new GameLoadInfo(
                    new GameSpawn(-1087, 47, 715),
                    new GameSpawn(-1083, 47, 703),
                    new GameSpawn(-1071, 47, 699),
                    new GameSpawn(-1060, 47, 703),
                    new GameSpawn(-1055, 47, 715),
                    new GameSpawn(-1060, 47, 726),
                    new GameSpawn(-1071, 47, 731),
                    new GameSpawn(-1083, 47, 726)
            )
    ),
    MinecartRacing(
            GameMinecartRacing.class, "Minecart Racing", Material.MINECART,
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 120,
            new GameLoadInfo(
                    new GameSpawn(-1723, 50, -2073),
                    new GameSpawn(-1721, 50, -2073),
                    new GameSpawn(-1719, 50, -2073),
                    new GameSpawn(-1717, 50, -2073),
                    new GameSpawn(-1715, 50, -2073),
                    new GameSpawn(-1713, 50, -2073),
                    new GameSpawn(-1711, 50, -2073),
                    new GameSpawn(-1709, 50, -2073)
            )
    ),
    /*
    PigFishing(
            GamePigFishing.class, "Pig Fishing", Items.FISHING_ROD,
            GameStyle.POINTS, 75,
            new GameLoadInfo(
                    new BlockPos(436, 27, -1877),
                    new GameSpawn[]{
                            new GameSpawn(435, 27, -1875, Direction.EAST),
                            new GameSpawn(435, 27, -1864, Direction.EAST),
                            new GameSpawn(444, 27, -1855, Direction.NORTH),
                            new GameSpawn(455, 27, -1855, Direction.NORTH),
                            new GameSpawn(464, 27, -1864, Direction.WEST),
                            new GameSpawn(464, 27, -1875, Direction.WEST),
                            new GameSpawn(455, 27, -1884, Direction.SOUTH),
                            new GameSpawn(444, 27, -1884, Direction.SOUTH),
                    }
            )
    ),
    ShootingRange(
            GameShootingRange.class, "Shooting Range", Items.BOW,
            GameStyle.POINTS, 120,

            new GameLoadInfo(
                    new BlockPos(1141, 30, 726),
                    new GameSpawn[]{
                            new GameSpawn(1141, 30, 748, Direction.WEST),
                            new GameSpawn(1141, 30, 746, Direction.WEST),
                            new GameSpawn(1141, 30, 744, Direction.WEST),
                            new GameSpawn(1141, 30, 742, Direction.WEST),
                            new GameSpawn(1141, 30, 740, Direction.WEST),
                            new GameSpawn(1141, 30, 738, Direction.WEST),
                            new GameSpawn(1141, 30, 736, Direction.WEST),
                            new GameSpawn(1141, 30, 734, Direction.WEST),
                            new GameSpawn(1141, 30, 732, Direction.WEST),
                            new GameSpawn(1141, 30, 730, Direction.WEST),
                            new GameSpawn(1141, 30, 728, Direction.WEST),
                            new GameSpawn(1141, 30, 726, Direction.WEST),
                            new GameSpawn(1141, 30, 724, Direction.WEST),
                            new GameSpawn(1141, 30, 722, Direction.WEST),
                            new GameSpawn(1141, 30, 720, Direction.WEST),
                            new GameSpawn(1141, 30, 718, Direction.WEST),
                    }
            )
    ),
    SpiderMaze(
            GameSpiderMaze.class, "Spider Maze", Items.SPIDER_SPAWN_EGG,
            GameStyle.RACE, 120,
            new GameLoadInfo(
                    new BlockPos(44, 1, 2098),
                    GameSpiderMaze.SPAWNS
            )
    ),
    SuperSheep(
            GameSuperSheep.class, "Super Sheep", Items.WHITE_WOOL,
            GameStyle.SURVIVAL, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new BlockPos(-911, 23, -3609),
                    new GameSpawn[] {
                            new GameSpawn(-942, 2, -3580),
                            new GameSpawn(-942, 2, -3608),
                            new GameSpawn(-912, 2, -3580),
                            new GameSpawn(-880, 2, -3580), // Reflected
                            new GameSpawn(-880, 2, -3638), // Reflected
                            new GameSpawn(-942, 2, -3638), // Reflected
                            new GameSpawn(-880, 2, -3610), // Reflected
                            new GameSpawn(-910, 2, -3638)  // Reflected
                    }
            )
    ),
    */
    TheFloorIsLava(
            GameTheFloorIsLava.class, "The Floor Is Lava", Material.COBBLESTONE_STAIRS,
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 90,
            new GameLoadInfo(
                    new GameSpawn(-181, 7, 2293, BlockFace.WEST)
            )
    ),
    Trampolinio(
            GameTrampolinio.class, "Trampolinio", Material.SLIME_BLOCK,
            GameStyle.POINTS, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 60,
            new GameLoadInfo(
                    new GameSpawn(GameTrampolinio.CENTER_BLOCK_POS)
            )
    ),
    Volcano(
            GameVolcano.class, "Volcano", Material.LAVA_BUCKET,
            GameStyle.SURVIVAL, GameRecordStyle.LONGEST_TIME, GameMode.ADVENTURE, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new GameSpawn(GameVolcano.CENTER_BLOCK_POS)
            )
    );

    public static final int MAX_GAME_DURATION = 60 * 60 * 100; // 100 hours

    public final Class<? extends Game> cls;
    public final String properName;
    public final String snakeCaseName;
    public final ItemStack iconItem;
    public final GameStyle style;
    public final GameRecordStyle recordStyle;
    public final GameMode gameMode;
    public final int maxDurationSecs;
    public final GameLoadInfo loadInfo;

    public final GameVariant[] variants;

    GameConfig(
            Class<? extends Game> cls,
            String properName,
            Material iconItem,
            GameStyle gameStyle,
            GameRecordStyle recordStyle,
            GameMode gameMode,
            int maxDurationSecs,
            GameLoadInfo loadInfo
    ) {
        this(cls, properName, new ItemStack(iconItem), gameStyle, recordStyle, gameMode, maxDurationSecs, loadInfo);
    }

    GameConfig(
            Class<? extends Game> cls,
            String properName, ItemStack iconItem,
            GameStyle gameStyle,
            GameRecordStyle recordStyle,
            GameMode gameMode,
            int maxDurationSecs,
            GameLoadInfo loadInfo
    ) {
        this.cls = cls;
        this.properName = properName;
        this.snakeCaseName = properName.toLowerCase().replace(" ", "_");
        this.iconItem = iconItem;
        this.style = gameStyle;
        this.recordStyle = recordStyle;
        this.gameMode = gameMode;
        this.maxDurationSecs = maxDurationSecs;
        this.loadInfo = loadInfo;

        var variants = new ArrayList<GameVariant>();
        variants.add(GameVariant.NONE);
        for (var field : cls.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                if (field.getType().equals(GameVariant.class)) {
                    try {
                        variants.add((GameVariant) field.get(null));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        this.variants = variants.toArray(GameVariant[]::new);
    }

    public int maxDurationTicks() {
        return maxDurationSecs * 20;
    }

    public static GameConfig fromName(String name) {
        for (GameConfig type : GameConfig.values()) {
            if (type.name().equalsIgnoreCase(name))
                return type;
            if (type.properName.equalsIgnoreCase(name))
                return type;
        }

        return null;
    }
}

