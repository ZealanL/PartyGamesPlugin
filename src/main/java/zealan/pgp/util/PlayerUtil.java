package zealan.pgp.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;
import net.minecraft.server.v1_8_R3.PacketPlayOutAttachEntity;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
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

        // Clear inv and reset hotbar slot
        player.getInventory().clear();
        player.getInventory().setHeldItemSlot(0);

        // Reset attributes
        player.setWalkSpeed(0.2f);
        player.setAllowFlight(player.getGameMode() == GameMode.CREATIVE);
        player.setFlying(false);
        player.setFlySpeed(0.05f);

        // Clear XP
        player.setLevel(0);
        player.setExp(0);

        // Remove fire
        player.setFireTicks(0);

        // Fix collision
        player.spigot().setCollidesWithEntities(true);

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

    public static void syncMount(Player player, Entity vehicle) {
        vehicle.setPassenger(player);

        PacketPlayOutAttachEntity attachPacket = new PacketPlayOutAttachEntity(
                0,
                ((CraftPlayer) player).getHandle(),
                ((CraftEntity) vehicle).getHandle()
        );

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(attachPacket);
    }

    public static void sendAbilities(Player player) {
        boolean invulnerable = false; // TODO: Implement
        boolean isFlying = player.isFlying();
        boolean allowFlight = player.getAllowFlight();
        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        float flySpeed = player.getFlySpeed();
        float walkSpeed = player.getWalkSpeed();

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerPlayerAbilities(
                invulnerable,
                isFlying,
                allowFlight,
                isCreative,
                flySpeed,
                walkSpeed
        ));
    }
}
