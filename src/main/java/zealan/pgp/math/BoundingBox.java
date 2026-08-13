package zealan.pgp.math;

import org.bukkit.util.Vector;

public class BoundingBox {
    public final Vector min;
    public final Vector max;

    public BoundingBox(Vector min, Vector max) {
        this.min = min;
        this.max = max;
    }

    public BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.min = new Vector(minX, minY, minZ);
        this.max = new Vector(maxX, maxY, maxZ);
    }

    public boolean contains(Vector v) {
        return min.getX() <= v.getX() &&
                min.getY() <= v.getY() &&
                min.getZ() <= v.getZ() &&
                max.getX() >= v.getX() &&
                max.getY() >= v.getY() &&
                max.getZ() >= v.getZ();
    }

    public BoundingBox offset(Vector v) {
        return new BoundingBox(min.add(v), max.add(v));
    }
}
