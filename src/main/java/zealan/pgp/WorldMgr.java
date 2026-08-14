package zealan.pgp;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.WorldUtil;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static zealan.pgp.Globals.PLOG;
import static zealan.pgp.Globals.PLUGIN;

public class WorldMgr extends AutoListener {
    private final HashSet<World> fixedWorlds = new HashSet<>();
    private final ArrayList<World> tempWorlds = new ArrayList<>();
    private final static Random RAND = new Random();

    private final static String TEMP_WORLD_PREFIX = "temp-world";
    
    private static List<String> getClosestRegionFiles(int blockX, int blockY) {
        int relX = Math.floorMod(blockX, 512);
        int relY = Math.floorMod(blockY, 512);
        int offsetX = relX >= 256 ? 1 : -1;
        int offsetZ = relY >= 256 ? 1 : -1;

        int px = Math.floorDiv(blockX, 512);
        int pz = Math.floorDiv(blockY, 512);

        int nx = px + offsetX;
        int nz = pz + offsetZ;

        return List.of(
                String.format("r.%d.%d.mca", px, pz),
                String.format("r.%d.%d.mca", nx, pz),
                String.format("r.%d.%d.mca", px, nz),
                String.format("r.%d.%d.mca", nx, nz)
        );
    }

    public WorldMgr() {
        var worldsDir = Bukkit.getWorldContainer().toPath();
        try (var stream = Files.walk(worldsDir, 1)) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile).forEach(
                            (file) -> {
                                if (file.isDirectory() && file.getName().startsWith(TEMP_WORLD_PREFIX)) {
                                    PLOG.info("Deleting temporary world: \"" + file.getName() + "\"");
                                    deleteDir(file.toPath());
                                }
                            }
                    );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private World createTempWorldAroundInner(Vec3i aroundBlockPos) throws IOException {
        Path mainWorldPath = WorldUtil.getWorldDir(WorldUtil.getMainWorld().getName()).toPath();
        String tempWorldName = TEMP_WORLD_PREFIX + RAND.nextInt();
        Path tempWorldPath = Bukkit.getWorldContainer().toPath().resolve(tempWorldName);
        PLOG.info("Creating temporary world \"" + tempWorldName + "\"...");

        var allowedRegionFiles = getClosestRegionFiles(aroundBlockPos.x, aroundBlockPos.z);

        // Copy world files
        {
            Files.createDirectories(tempWorldPath);
            Files.walkFileTree(mainWorldPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path targetDir = tempWorldPath.resolve(mainWorldPath.relativize(dir));
                    Files.createDirectories(targetDir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    var dirName = file.getParent().getFileName().toString();
                    if (!dirName.equalsIgnoreCase(WorldUtil.getMainWorld().getName()) &&
                            !dirName.equalsIgnoreCase("region") &&
                            !dirName.equalsIgnoreCase("data")
                    ) {
                        return FileVisitResult.CONTINUE;
                    }

                    var fileName = file.getFileName().toString().toLowerCase();
                    if (fileName.equals("session.lock") || fileName.equals("uid.dat")) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (fileName.endsWith(".mca")) {
                        boolean isAllowed = allowedRegionFiles.stream()
                                .anyMatch(s -> s.equalsIgnoreCase(fileName));
                        if (!isAllowed)
                            return FileVisitResult.CONTINUE;
                    }

                    PLOG.info(
                            " > Copying \"" + file.getFileName() +
                                    "\" to \"" + mainWorldPath.relativize(file) + "\""
                    );
                    Files.copy(
                            file,
                            tempWorldPath.resolve(mainWorldPath.relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        WorldCreator creator = new WorldCreator(tempWorldName);
        var world = Bukkit.createWorld(creator);
        world.setAutoSave(false);
        world.setDifficulty(Difficulty.NORMAL);

        // Remove existing entities
        var entities = world.getEntities();
        for (var entity : entities)
            entity.remove();

        return world;
    }

    public World createTempWorldAround(Vec3i aroundBlockPos) {
        PLOG.info("Creating new blank runtime world...");
        try {
            World newWorld = createTempWorldAroundInner(aroundBlockPos);
            this.tempWorlds.add(newWorld);
            return newWorld;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteDir(Path dir) {
        try {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        } catch (IOException e) {
            PLOG.severe("Removing directoy failed: " + e.getMessage());
        }
    }

    private void removeOldTempWorlds() {
        tempWorlds.removeIf(
                world -> {
                    boolean shouldRemove = world.getPlayers().isEmpty();

                    if (shouldRemove) {
                        var worldDir = WorldUtil.getWorldDir(world.getName());
                        Bukkit.unloadWorld(world, false);

                        try {
                            try (var stream = Files.walk(worldDir.toPath())) {
                                stream.sorted(Comparator.reverseOrder())
                                        .map(Path::toFile)
                                        .forEach(java.io.File::delete);
                            }
                        } catch (IOException e) {
                            PLOG.severe("Removing temp world failed: " + e.getMessage());
                        }
                    }

                    return shouldRemove;
                }
        );
    }

    private void fixWorld(World world) {
        if (!fixedWorlds.add(world))
            return;

        world.setAutoSave(world == WorldUtil.getMainWorld());
        world.setDifficulty(Difficulty.NORMAL);

        // Disable spawning updates
        world.setAmbientSpawnLimit(1);
        world.setAnimalSpawnLimit(1);
        world.setTicksPerAnimalSpawns(999999);
        world.setTicksPerMonsterSpawns(999999);

        world.setTime(23000);
        {
            world.setGameRuleValue("doDaylightCycle", "false");
            world.setGameRuleValue("doWeatherCycle", "false");
            world.setGameRuleValue("randomTickSpeed", "0");
            world.setGameRuleValue("doFireTick", "false");

            world.setGameRuleValue("doMobSpawning", "false");
            world.setGameRuleValue("doMobLoot", "false");
            world.setGameRuleValue("mobGriefing", "false");

            world.setGameRuleValue("doEntityDrops", "false");
            world.setGameRuleValue("doTileDrops", "false");

            world.setGameRuleValue("naturalRegeneration", "false");
            world.setGameRuleValue("showDeathMessages", "false");
        }
    }

    @Override
    public void onTick() {
        removeOldTempWorlds();
        // Clear weather
        for (var world : Bukkit.getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(0);
        }
    }

    @EventHandler
    void handle(ChunkLoadEvent event) {
        World world = event.getWorld();
        fixWorld(world);
    }

    @EventHandler
    void handle(ChunkUnloadEvent event) {
        if (event.getWorld() != WorldUtil.getMainWorld()) {
            event.getChunk().unload(false, false);
            event.setCancelled(true); // Prevent main event
        }
    }

    @EventHandler
    void handle(PluginDisableEvent event) {
        for (World world : this.tempWorlds) {
            Bukkit.unloadWorld(world, false);
        }
        tempWorlds.clear();
    }
}
