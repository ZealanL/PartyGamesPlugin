package zealan.pgp;

import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import static zealan.pgp.Globals.PLUGIN;

public class AutoListener implements Listener {
    private volatile boolean registered = false;
    private int totalTicksFired = 0;
    public AutoListener() {
        PLUGIN.getServer().getPluginManager().registerEvents(this, PLUGIN);

        new BukkitRunnable() {
            @Override
            public void run() {
                onTick();
                totalTicksFired++;
            }
        }.runTaskTimer(PLUGIN, 0L, 1L);

        registered = true;
    }

    public void onTick(){}

    public final int getListenerTickCount() {
        return totalTicksFired;
    }
}
