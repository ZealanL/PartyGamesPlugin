package zealan.pgp.armorstand;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class FakeArmorStand {
    private static final Random RAND = new Random();
    private static final int TRACKING_RANGE = 32;

    private final Location location;
    private final HashSet<Player> trackingPlayers = new HashSet<>();
    private final int fakeEntityId;
    private final UUID fakeEntityUUID;

    private String customName = "";
    private boolean customNameDirty = false;

    public FakeArmorStand(Location location) {
        this.location = location;

        this.fakeEntityId = RAND.nextInt();
        this.fakeEntityUUID = UUID.randomUUID();
    }

    public void setName(String name) {
        if (customName.equals(name))
            return;

        customName = name;
        customNameDirty = true;
    }

    public void clearName() {
        setName(null);
    }

    private void sendSpawnPacket(Player player) {
        var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return;
        user.sendPacket(
                new WrapperPlayServerSpawnLivingEntity(
                        fakeEntityId,
                        fakeEntityUUID,
                        EntityTypes.ARMOR_STAND,
                        new Vector3d(location.getX(), location.getY(), location.getZ()),
                        0f, 0f, 0f, // Rotation
                        new Vector3d(0, 0, 0), // Velocity
                        List.of(
                                // Invisible with no particles
                                //new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20),
                                //new EntityData(8, EntityDataTypes.BYTE, (byte) 1),
                                //new EntityData(9, EntityDataTypes.BYTE, (byte) 0)
                        )
                )
        );
    }

    private void sendMetadataPacket(Player player) {
        var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return;
        user.sendPacket(new WrapperPlayServerEntityMetadata(
                fakeEntityId,
                List.of(
                        // Custom name + custom name visible
                        new EntityData(2, EntityDataTypes.STRING, customName),
                        new EntityData(3, EntityDataTypes.BYTE, (byte)(customName.isEmpty() ? 0 : 1))
                )
        ));
    }

    private void sendDestroyPacket(Player player) {
        var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return;
        user.sendPacket(
                new WrapperPlayServerDestroyEntities(fakeEntityId)
        );
    }

    public void updateToPlayers() {
        HashSet<Player> playersInRange = Bukkit.getOnlinePlayers().stream().filter(
                player ->
                    player.getWorld() == location.getWorld() && player.getLocation().distance(location) <= TRACKING_RANGE
        ).collect(Collectors.toCollection(HashSet::new));

        // Add new players
        for (Player player : playersInRange) {
            if (trackingPlayers.contains(player))
                continue;

            sendSpawnPacket(player);
            sendMetadataPacket(player);

            trackingPlayers.add(player);
        }

        trackingPlayers.removeIf(player -> {
            if (!playersInRange.contains(player)) {
                sendDestroyPacket(player);
                return true;
            } else {
                return false;
            }
        });

        if (customNameDirty) {
            for (var player : trackingPlayers)
                sendMetadataPacket(player);
            customNameDirty = false;
        }

    }
}
