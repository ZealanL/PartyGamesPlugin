package zealan.pgp.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;


public class PlayerUtil {
    public static void cleanPlayer(Player player) {
        player.setVelocity(new Vector(0, 0, 0));

        // Reset health
        player.setHealth(player.getMaxHealth());
        player.setMaxHealth(20.0);

        // Clear effects
        for (PotionEffect effect : player.getActivePotionEffects())
            player.removePotionEffect(effect.getType());

        // Clear inv
        player.getInventory().clear();

        // Reset attributes
        player.setWalkSpeed(0.2f);
        player.setAllowFlight(player.getGameMode() == GameMode.CREATIVE);
        player.setFlying(false);

        // Clear XP
        player.setLevel(0);
        player.setExp(0);

        // Remove fire
        player.setFireTicks(0);

        if (player.getVehicle() != null)
            player.getVehicle().remove();
    }

    public static void makeSwingHand(Player player) {
        var packet = new WrapperPlayServerEntityAnimation(
                player.getEntityId(), WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
        );

        for (var p : player.getWorld().getPlayers())
            PacketEvents.getAPI().getPlayerManager().sendPacket(p, packet);
    }
}
