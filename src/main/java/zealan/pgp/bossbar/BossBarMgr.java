package zealan.pgp.bossbar;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import zealan.pgp.AutoListener;
import zealan.pgp.api.display.Display;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class BossBarMgr extends AutoListener {
    private static final Random RAND = new Random();

    private static class FakeWither {
        final int entityId;
        final UUID uuid;
        final Player player;
        final User user;

        String title = "";
        float frac = 0;
        int age = 0;

        FakeWither(Player player) {
            this.entityId = Math.abs(RAND.nextInt());
            this.uuid = UUID.randomUUID();
            this.player = player;
            this.user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        }

        static float calcHealth(float frac) {
            return Math.max(0.5f, 300 * frac);
        }

        private Vector calcIdealPos() {
            final float TELEPORT_DIST = 50;
            var playerLoc = player.getLocation();
            return playerLoc.toVector().add(
                    playerLoc.getDirection().multiply(TELEPORT_DIST)
            );
        }

        void sendSpawnPacket() {
            var pos = calcIdealPos();
            user.sendPacket(new WrapperPlayServerSpawnLivingEntity(
                    entityId,
                    uuid,
                    EntityTypes.WITHER,
                    new Vector3d(pos.getX(), pos.getY(), pos.getZ()),
                    0f, 0f, 0f, // Rotation
                    new Vector3d(0, 0, 0),
                    List.of(
                            // Invisible with no particles
                            new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20),
                            new EntityData(8, EntityDataTypes.BYTE, (byte) 1),
                            new EntityData(9, EntityDataTypes.BYTE, (byte) 0),

                            new EntityData(20, EntityDataTypes.INT, 0),

                            // Custom name + custom name visible
                            new EntityData(2, EntityDataTypes.STRING, title),
                            new EntityData(3, EntityDataTypes.BYTE, (byte) 1),

                            // Heath
                            new EntityData(6, EntityDataTypes.FLOAT, calcHealth(frac))
                    )
            ));
        }

        void sendTeleportPacket() {
            var pos = calcIdealPos();
            user.sendPacket(new WrapperPlayServerEntityTeleport(
                    entityId,
                    new Vector3d(pos.getX(), pos.getY(), pos.getZ()),
                    0f, 0f, false
            ));
        }

        void sendUpdateDataPacket() {
            user.sendPacket(new WrapperPlayServerEntityMetadata(
                    entityId,
                    List.of(
                            // Custom name + custom name visible
                            new EntityData(2, EntityDataTypes.STRING, title),
                            new EntityData(3, EntityDataTypes.BYTE, (byte) 1),
                            // Heath
                            new EntityData(6, EntityDataTypes.FLOAT, calcHealth(frac))
                    )
            ));
        }

        void sendRemovePacket() {
            user.sendPacket(new WrapperPlayServerDestroyEntities(entityId));
        }
    }

    private final HashMap<Player, FakeWither> fakeWithers = new HashMap<>();
    private static final int MAX_AGE = 10;

    public void setForPlayer(Player player, BossBarContent content) {
        if (!player.isOnline())
            return;

        var existing = fakeWithers.get(player);
        if (existing != null) {
            existing.title = Display.format(content.title);
            existing.frac = (float) content.frac;
        } else {
            var newWither = new FakeWither(player);
            if (newWither.user == null)
                return;
            newWither.title = Display.format(content.title);
            newWither.frac = (float) content.frac;
            newWither.sendSpawnPacket();
            fakeWithers.put(player, newWither);
        }
    }

    @Override
    public void onTick() {
        fakeWithers.entrySet().removeIf(entry -> entry.getValue().user == null);

        var toRemove = fakeWithers.entrySet().stream().filter(
                entry -> entry.getValue().age >= MAX_AGE
        ).toList();
        for (var entry : toRemove) {
            entry.getValue().sendRemovePacket();
            fakeWithers.remove(entry.getKey());
        }

        for (var entry : fakeWithers.entrySet()) {
            FakeWither fakeWither = entry.getValue();
            fakeWither.sendUpdateDataPacket();
            fakeWither.sendTeleportPacket();
            fakeWither.age++;
        }
    }

    @EventHandler
    void handle(PlayerQuitEvent quitEvent) {
        fakeWithers.remove(quitEvent.getPlayer());
    }
}
