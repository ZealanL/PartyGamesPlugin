package zealan.pgp;

import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import zealan.pgp.util.WorldUtil;

import java.util.ArrayList;
import java.util.HashSet;

import static zealan.pgp.Globals.PLAYER_MGR;
import static zealan.pgp.Globals.PLOG;

public class WorldMgr extends AutoListener {
    private final HashSet<World> fixedWorlds = new HashSet<>();
    private final ArrayList<World> tempWorlds = new ArrayList<>();
    private final static int MAX_RUNTIME_WORLDS = 12;

    private World createBlankWorldSlot(String name) {
        WorldCreator creator = new WorldCreator(name);

        creator.type(WorldType.FLAT);
        creator.generatorSettings("{\"layers\": [], \"biome\": \"minecraft:plains\"}");
        creator.generateStructures(false);

        World world = Bukkit.createWorld(creator);
        if (world == null)
            throw new RuntimeException("Failed to initialize Bukkit world slot: " + name);

        world.setAutoSave(false);
        fixWorld(world);

        return world;
    }

    public World getUnusedWorld() {
        for (World world : this.tempWorlds) {
            boolean hasPlayers = false;
            for (var player : world.getPlayers()) {
                if (PLAYER_MGR.isFakeSpectator(player))
                    continue;

                hasPlayers = true;
                break;
            }
            if (!hasPlayers)
                return world;
        }

        if (this.tempWorlds.size() < MAX_RUNTIME_WORLDS) {
            PLOG.info("Creating new blank runtime world...");
            int slotId = tempWorlds.size() + 1;
            World newWorld = createBlankWorldSlot("temp_world_slot" + slotId);
            this.tempWorlds.add(newWorld);
            return newWorld;
        } else {
            throw new RuntimeException("Ran out of world slots (max=" + MAX_RUNTIME_WORLDS + ")");
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

    @EventHandler
    void handle(ChunkLoadEvent event) {
        World world = event.getWorld();
        fixWorld(world);
    }

    @EventHandler
    void handle(ChunkUnloadEvent event) {
        if (event.getWorld() != WorldUtil.getMainWorld()) {
            event.getChunk().unload(false, true);
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
