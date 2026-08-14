package zealan.pgp.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.MathHelper;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFallingSand;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import zealan.pgp.math.BoundingBox;
import zealan.pgp.math.Vec3i;

public class EntityUtil {
    public static BoundingBox getEntityHitbox(Entity ent) {
        CraftEntity craftEntity = (CraftEntity) ent;

        var nmsEntity = craftEntity.getHandle();
        var aabb = nmsEntity.getBoundingBox();

        return new BoundingBox(aabb.a, aabb.b, aabb.c, aabb.d, aabb.e, aabb.f);
    }

    public static void kill(Entity ent) {
        if (ent instanceof LivingEntity living) {
            living.damage(living.getMaxHealth() * 100);
        } else {
            ent.remove();
        }
    }

    public static Vec3i getSupportingBlockPos(Entity entity) {
        World world = entity.getWorld();
        var aabb = ((CraftEntity) entity).getHandle().getBoundingBox();

        double minY = aabb.b - 0.1;

        int minX = MathHelper.floor(aabb.a);
        int maxX = MathHelper.floor(aabb.d);
        int minZ = MathHelper.floor(aabb.c);
        int maxZ = MathHelper.floor(aabb.f);
        int blockY = MathHelper.floor(minY);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = world.getBlockAt(x, blockY, z);
                if (block.getType().isSolid())
                    return new Vec3i(block.getX(), block.getY(), block.getZ());
            }
        }

        return Vec3i.from(
                entity.getLocation().toVector().add(new Vector(0.0, -0.01, 0.0))
        );
    }

    public static void spoofEntityMove(Entity ent, Vector pos, boolean relative) {
        PacketWrapper<?> packet;
        if (!relative) {
            packet = new WrapperPlayServerEntityTeleport(
                    ent.getEntityId(),
                    new Vector3d(pos.getX(), pos.getY(), pos.getZ()),
                    0, 0, false
            );
        } else {
            packet = new WrapperPlayServerEntityRelativeMove(
                    ent.getEntityId(),
                    pos.getX(), pos.getY(), pos.getZ(),
                    false
            );
        }

        for (var player : ent.getWorld().getPlayers()) {
            var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user != null)
                user.sendPacket(packet);
        }
    }

    public static void setSilent(Entity entity, boolean silent) {
        ((CraftEntity)entity).getHandle().b(silent);
    }

    public static void setNoAI(LivingEntity entity, boolean noAI) {
        var nms = ((CraftLivingEntity)entity).getHandle();
        if (nms instanceof EntityInsentient entityInsentient) {
            entityInsentient.k(noAI);
        } else {
            throw new IllegalArgumentException("Entity does not inherit from insentient class");
        }
    }

    public static void makeInvisible(LivingEntity entity) {
        entity.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                Integer.MAX_VALUE,
                1,
                false,
                false
        ));
    }

    public static void keepFallingBlockAlive(FallingBlock fbe) {
        ((CraftFallingSand)fbe).getHandle().ticksLived = 1;
    }

    public static void setYaw(Entity entity, float yaw) {
        ((CraftEntity)entity).getHandle().yaw = yaw;
    }

    public static void setHeadYaw(LivingEntity entity, float yaw) {
        ((CraftLivingEntity)entity).getHandle().aK = yaw;
    }
}
