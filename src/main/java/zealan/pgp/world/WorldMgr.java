package zealan.pgp.world;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import zealan.pgp.AutoListener;
import zealan.pgp.math.BlockRange2d;
import zealan.pgp.util.WorldUtil;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static zealan.pgp.Globals.PLOG;

public class WorldMgr extends AutoListener {
    private final HashSet<World> fixedWorlds = new HashSet<>();
    private final ArrayList<World> tempWorlds = new ArrayList<>();
    private final static Random RAND = new Random();

    private final static String TEMP_WORLD_PREFIX = "temp-world";

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

    private World getOrMakeTempWorld() {
        for (var world : tempWorlds) {
            if (world.getPlayers().isEmpty()) {
                for (var entity : world.getEntities()) {
                    entity.remove();
                }

                return world;
            }
        }

        PLOG.info("Creating new blank runtime world...");
        String tempWorldName = TEMP_WORLD_PREFIX + "_" + RAND.nextInt();
        WorldCreator creator = new WorldCreator(tempWorldName);
        creator.generateStructures(false);
        creator.type(WorldType.FLAT);
        creator.generatorSettings("2;0;1;");
        var world = Bukkit.createWorld(creator);
        world.setAutoSave(false);
        world.setDifficulty(Difficulty.NORMAL);
        tempWorlds.add(world);
        return world;
    }

    public World getOrMakeTempWorldOf(BlockRange2d loadRange) {
        try {
            World tempWorld = getOrMakeTempWorld();

            FastWorldCopy.copyRange(WorldUtil.getMainWorld(), tempWorld, loadRange);
            return tempWorld;
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

        world.setPVP(true);

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
        // Clear weather
        for (var world : Bukkit.getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(0);
        }
    }

    @EventHandler
    void handle(PluginDisableEvent event) {
        for (World world : this.tempWorlds) {
            Bukkit.unloadWorld(world, false);
        }
        tempWorlds.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        fixWorld(world);

        for (Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }
    }
}
