package zealan.pgp.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import zealan.pgp.math.Vec3i;

import java.io.File;

public class WorldUtil {
    public static World getMainWorld() {
        return Bukkit.getWorlds().get(0);
    }

    public static File getWorldDir(String name) {
        return new File(Bukkit.getWorldContainer(), name);
    }

    public static Block getBlock(World world, Vec3i pos) {
        return world.getBlockAt(pos.x, pos.y, pos.z);
    }
}
