package zealan.pgp.math;

import org.bukkit.util.Vector;

public class Vec3i {
    public int x;
    public int y;
    public int z;

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Vec3i from(Vector vector) {
        return new Vec3i(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    public Vector getBottomCenter() {
        return new Vector(this.x + 0.5, this.y, this.z + 0.5);
    }

    public Vector getCenter() {
        return new Vector(this.x + 0.5, this.y + 0.5, this.z + 0.5);
    }

    public Vec3i add(int x, int y, int z) {
        return new Vec3i(this.x + x, this.y + y, this.z + z);
    }

    public Vec3i add(Vec3i other) {
        return new Vec3i(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public Vec3i subtract(int x, int y, int z) {
        return new Vec3i(this.x - x, this.y - y, this.z - z);
    }

    public Vec3i subtract(Vec3i other) {
        return new Vec3i(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vec3i multiply(int factor) {
        return new Vec3i(this.x * factor, this.y * factor, this.z * factor);
    }

    public Vec3i multiply(int x, int y, int z) {
        return new Vec3i(this.x * x, this.y * y, this.z * z);
    }

    public Vec3i withX(int x) {
        return new Vec3i(x, this.y, this.z);
    }
    public Vec3i withY(int y) {
        return new Vec3i(this.x, y, this.z);
    }
    public Vec3i withZ(int z) {
        return new Vec3i(this.x, this.y, z);
    }

    public Vec3i up() {
        return this.withY(y + 1);
    }
    public Vec3i down() {
        return this.withY(y - 1);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + this.x;
        hash = 31 * hash + this.y;
        hash = 31 * hash + this.z;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vec3i other = (Vec3i) obj;
        return this.x == other.x && this.y == other.y && this.z == other.z;
    }
}
