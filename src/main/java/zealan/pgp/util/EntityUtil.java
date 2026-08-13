package zealan.pgp.util;

import net.minecraft.server.v1_8_R3.MathHelper;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
}
