package zealan.pgp.game;

import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import zealan.pgp.game.games.*;
import zealan.pgp.math.BlockRange2d;
import zealan.pgp.util.ItemUtil;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

public enum GameConfig {
    AnimalSlaughter(
            GameAnimalSlaughter.class, "Animal Slaughter", Material.WOOD_SWORD,
            GameStyle.POINTS, GameRecordStyle.POINTS, GameMode.ADVENTURE, 60,
            new GameLoadInfo(
                    new BlockRange2d(-1090, -1819, -958, -1952),
                    new GameSpawn(GameAnimalSlaughter.CENTER_BLOCK_POS)
            )
    ),
    AnvilSpleef(
            GameAnvilSpleef.class, "Anvil Spleef", Material.ANVIL,
            GameStyle.SURVIVAL, GameRecordStyle.LONGEST_TIME, GameMode.ADVENTURE, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new BlockRange2d(-266, -3589, -222, -3631),
                    new GameSpawn(-243, 1, -3608)
            )
    ),
    Avalanche(
            GameAvalanche.class, "Avalanche", Material.SNOW_BLOCK,
            GameStyle.SURVIVAL, GameRecordStyle.LONGEST_TIME, GameMode.ADVENTURE, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new BlockRange2d(-2415, -1859, -2371, -1906),
                    new GameSpawn(-2394, 45, -1881)
            )
    ),
    Bombardment(
            GameBombardment.class, "Bombardment", Material.COAL_BLOCK,
            GameStyle.SURVIVAL, GameRecordStyle.LONGEST_TIME, GameMode.ADVENTURE, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new BlockRange2d(-453, 800, -300, 650),
                    new GameSpawn(-392, 20, 726, BlockFace.EAST)
            )
    ),
    CannonPainting(
            GameCannonPainting.class, "Cannon Painting", Material.EGG,
            GameStyle.POINTS, GameRecordStyle.POINTS, GameMode.ADVENTURE, 30,
            new GameLoadInfo(
                    new BlockRange2d(-191, 1466, 230, 1420),
                    new GameSpawn(217, 19, 1443)
            )
    ),

    ChickenRings(
            GameChickenRings.class, "Chicken Rings", ItemUtil.getSpawnEgg(EntityType.CHICKEN),
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 120,
            new GameLoadInfo(
                    new BlockRange2d(2392, 574, 2700, -633),
                    new GameSpawn(2492, 71, 220, BlockFace.NORTH)
            )
    ),
    Dive(
            GameDive.class, "Dive", Material.WATER_BUCKET,
            GameStyle.POINTS, GameRecordStyle.POINTS, GameMode.ADVENTURE, 60,
            new GameLoadInfo(
                    new BlockRange2d(900, -3560, 991, -3650),
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
    FrozenFloor(
            GameFrozenFloor.class, "Frozen Floor", Material.ICE,
            GameStyle.SURVIVAL, null, GameMode.ADVENTURE, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new BlockRange2d(-621, 1663, -517, 1559),

                    // TODO: Guesses from replays
                    new GameSpawn(-572, 59, 1591),
                    new GameSpawn(-572, 59, 1620),
                    new GameSpawn(-572, 59, 1615),
                    new GameSpawn(-575, 59, 1600),
                    new GameSpawn(-557, 59, 1596),
                    new GameSpawn(-556, 59, 1618),
                    new GameSpawn(-555, 59, 1607),
                    new GameSpawn(-567, 59, 1595)
            )
    ),
    HighGround(
            GameHighGround.class, "High Ground", ItemUtil.getColoredWool(DyeColor.RED),
            GameStyle.POINTS, null, GameMode.ADVENTURE, 60,
            new GameLoadInfo(
                    new BlockRange2d(315, 788, 447, 672),

                    new GameSpawn(374, 39, 734),
                    new GameSpawn(374, 39, 730),
                    new GameSpawn(379, 39, 722),
                    new GameSpawn(384, 39, 720),
                    new GameSpawn(388, 39, 720),
                    new GameSpawn(393, 39, 722),
                    new GameSpawn(396, 39, 725),
                    new GameSpawn(398, 39, 730),
                    new GameSpawn(398, 39, 734),
                    new GameSpawn(396, 39, 739),
                    new GameSpawn(393, 39, 742),
                    new GameSpawn(388, 39, 744),
                    new GameSpawn(384, 39, 744),
                    new GameSpawn(379, 39, 742),
                    new GameSpawn(376, 39, 739)

            )
    ),
    HoeHoeHoe(
            GameHoeHoeHoe.class, "Hoe Hoe Hoe", Material.DIAMOND_HOE,
            GameStyle.POINTS, GameRecordStyle.POINTS, GameMode.SURVIVAL, 60,
            new GameLoadInfo(
                    new BlockRange2d(1151, -1850, 1190, -1888),
                    new GameSpawn(1171, 112, -1867),
                    new GameSpawn(1171, 112, -1867),
                    new GameSpawn(1171, 112, -1867),
                    new GameSpawn(1171, 112, -1867),
                    new GameSpawn(1171, 112, -1867),
                    new GameSpawn(1171, 112, -1867),
                    new GameSpawn(1171, 112, -1867),
                    new GameSpawn(1171, 112, -1867)
            )
    ),
    JigsawRush(
            GameJigsawRush.class, "Jigsaw Rush", Material.DAYLIGHT_DETECTOR,
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 60,
            new GameLoadInfo(
                    new BlockRange2d(222, 1830, 270, 1800),
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
                    new BlockRange2d(-307, 2319, -175, 2302),
                    new GameSpawn(-181, 7, 2310, BlockFace.WEST)
            )
    ),
    LabEscape(
            GameLabEscape.class, "Lab Escape", Material.IRON_AXE,
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.SURVIVAL, 180,
            new GameLoadInfo(
                    new BlockRange2d(1407, -81, 1471, -144),
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
                    new BlockRange2d(-1124, 768, -1009, 656),
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
                    new BlockRange2d(-1729, -1648, -1701, -2082),
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
    PigFishing(
            GamePigFishing.class, "Pig Fishing", Material.FISHING_ROD,
            GameStyle.POINTS, GameRecordStyle.POINTS, GameMode.ADVENTURE, 75,
            new GameLoadInfo(
                    new BlockRange2d(418, -1840, 479, -1902),

                    new GameSpawn(435, 27, -1875, BlockFace.EAST),
                    new GameSpawn(435, 27, -1864, BlockFace.EAST),
                    new GameSpawn(444, 27, -1855, BlockFace.NORTH),
                    new GameSpawn(455, 27, -1855, BlockFace.NORTH),
                    new GameSpawn(464, 27, -1864, BlockFace.WEST),
                    new GameSpawn(464, 27, -1875, BlockFace.WEST),
                    new GameSpawn(455, 27, -1884, BlockFace.SOUTH),
                    new GameSpawn(444, 27, -1884, BlockFace.SOUTH)
            )
    ),
    PigJousting(
            GamePigJousting.class, "Pig Jousting", Material.SADDLE,
            GameStyle.SURVIVAL, null, GameMode.ADVENTURE, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new BlockRange2d(-2483, 752, -2430, 698),

                    new GameSpawn(-2470, 30, 748),
                    new GameSpawn(-2461, 30, 748),
                    new GameSpawn(-2452, 30, 748),
                    new GameSpawn(-2443, 30, 748),

                    new GameSpawn(-2480, 30, 711),
                    new GameSpawn(-2480, 30, 720),
                    new GameSpawn(-2480, 30, 729),
                    new GameSpawn(-2480, 30, 728),

                    new GameSpawn(-2470, 30, 701),
                    new GameSpawn(-2461, 30, 701),
                    new GameSpawn(-2452, 30, 701),
                    new GameSpawn(-2443, 30, 701),

                    new GameSpawn(-2433, 30, 711),
                    new GameSpawn(-2433, 30, 720),
                    new GameSpawn(-2433, 30, 729),
                    new GameSpawn(-2433, 30, 728)
            )
    ),
    ShootingRange(
            GameShootingRange.class, "Shooting Range", Material.BOW,
            GameStyle.POINTS, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 120,

            new GameLoadInfo(
                    new BlockRange2d(1071, 767, 1152, 689),
                    new GameSpawn[]{
                            new GameSpawn(1141, 30, 748, BlockFace.WEST),
                            new GameSpawn(1141, 30, 746, BlockFace.WEST),
                            new GameSpawn(1141, 30, 744, BlockFace.WEST),
                            new GameSpawn(1141, 30, 742, BlockFace.WEST),
                            new GameSpawn(1141, 30, 740, BlockFace.WEST),
                            new GameSpawn(1141, 30, 738, BlockFace.WEST),
                            new GameSpawn(1141, 30, 736, BlockFace.WEST),
                            new GameSpawn(1141, 30, 734, BlockFace.WEST),
                            new GameSpawn(1141, 30, 732, BlockFace.WEST),
                            new GameSpawn(1141, 30, 730, BlockFace.WEST),
                            new GameSpawn(1141, 30, 728, BlockFace.WEST),
                            new GameSpawn(1141, 30, 726, BlockFace.WEST),
                            new GameSpawn(1141, 30, 724, BlockFace.WEST),
                            new GameSpawn(1141, 30, 722, BlockFace.WEST),
                            new GameSpawn(1141, 30, 720, BlockFace.WEST),
                            new GameSpawn(1141, 30, 718, BlockFace.WEST),
                    }
            )
    ),
    SpiderMaze(
            GameSpiderMaze.class, "Spider Maze", ItemUtil.getSpawnEgg(EntityType.SPIDER),
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 120,
            new GameLoadInfo(
                    new BlockRange2d(-28, 2172, 117, 2027),
                    GameSpiderMaze.SPAWNS
            )
    ),
    SuperSheep(
            GameSuperSheep.class, "Super Sheep", ItemUtil.getSpawnEgg(EntityType.SHEEP),
            GameStyle.SURVIVAL, GameRecordStyle.LONGEST_TIME, GameMode.ADVENTURE, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new BlockRange2d(-961, -3559, -860, -3660),
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
    TheFloorIsLava(
            GameTheFloorIsLava.class, "The Floor Is Lava", Material.COBBLESTONE_STAIRS,
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 90,
            new GameLoadInfo(
                    new BlockRange2d(-305, 2302, -177, 2287),
                    new GameSpawn(-181, 7, 2293, BlockFace.WEST)
            )
    ),
    Trampolinio(
            GameTrampolinio.class, "Trampolinio", Material.SLIME_BLOCK,
            GameStyle.POINTS, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 60,
            new GameLoadInfo(
                    new BlockRange2d(2543,-1831,2623, -1915),
                    new GameSpawn(GameTrampolinio.CENTER_BLOCK_POS)
            )
    ),
    Volcano(
            GameVolcano.class, "Volcano", Material.LAVA_BUCKET,
            GameStyle.SURVIVAL, GameRecordStyle.LONGEST_TIME, GameMode.ADVENTURE, GameConfig.MAX_GAME_DURATION,
            new GameLoadInfo(
                    new BlockRange2d(-400, -1765, -175, -2014),
                    new GameSpawn(GameVolcano.CENTER_BLOCK_POS)
            )
    ),
    PunchTheBats(
            GamePunchTheBats.class, "Punch The Bats", ItemUtil.getSpawnEgg(EntityType.BAT),
            GameStyle.POINTS, GameRecordStyle.POINTS, GameMode.ADVENTURE, 60,
            new GameLoadInfo(
                    new BlockRange2d(1888, -1857, 1918, -1886),
                    new GameSpawn(GamePunchTheBats.CENTER_SPAWN_POS)
            )
    ),
    BoatRace(
            GameBoatRace.class, "Boat Race", Material.BOAT,
            GameStyle.RACE, GameRecordStyle.FASTEST_TIME, GameMode.ADVENTURE, 120,
            new GameLoadInfo(
                    new BlockRange2d(2428, -2418, 2722, -2504),

                    // TODO: Estimated based on limited video evidence
                    new GameSpawn(2449, 139, -2468 - 3, BlockFace.EAST),
                    new GameSpawn(2449, 139, -2468, BlockFace.EAST), // DanTDM spawned here-ish
                    new GameSpawn(2449, 139, -2468 + 3, BlockFace.EAST),
                    new GameSpawn(2449, 139, -2468 + 6, BlockFace.EAST),
                    new GameSpawn(2449, 139, -2468 + 9, BlockFace.EAST),
                    new GameSpawn(2449, 139, -2468 + 12, BlockFace.EAST),
                    new GameSpawn(2449, 139, -2468 + 15, BlockFace.EAST),
                    new GameSpawn(2449, 139, -2468 + 18, BlockFace.EAST)
            )
    )

    ;

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

