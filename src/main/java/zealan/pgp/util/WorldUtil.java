package zealan.pgp.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class WorldUtil {
    public static World getMainWorld() {
        return Bukkit.getWorlds().get(0);
    }
}
