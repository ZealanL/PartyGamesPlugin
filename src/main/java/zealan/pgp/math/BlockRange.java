package zealan.pgp.math;


import com.github.retrooper.packetevents.protocol.world.BoundingBox;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

public class BlockRange implements Iterable<Vec3i> {
    private final Vec3i min;
    private final Vec3i size;

    private BlockRange(Vec3i min, Vec3i size) {
        assert size.x > 0 && size.y > 0 && size.z > 0;
        this.min = min;
        this.size = size;
    }

    /// Creates a BlockRange from the two corners (including BOTH corners)
    public static BlockRange fromCorners(Vec3i a, Vec3i b) {
        Vec3i min = new Vec3i(
                Math.min(a.x, b.x),
                Math.min(a.y, b.y),
                Math.min(a.z, b.z)
        );
        Vec3i max = new Vec3i(
                Math.max(a.x, b.x),
                Math.max(a.y, b.y),
                Math.max(a.z, b.z)
        );
        Vec3i exclusiveMax = max.add(1, 1, 1);
        Vec3i size = exclusiveMax.subtract(min);
        return new BlockRange(min, size);
    }

    public static BlockRange fromCorners(int x1, int y1, int z1, int x2, int y2, int z2) {
        return BlockRange.fromCorners(new Vec3i(x1, y1, z1), new Vec3i(x2, y2, z2));
    }

    public Vec3i min() {
        return this.min;
    }
    public Vec3i maxExclusive() {
        return this.min.add(this.size);
    }
    public Vec3i maxInclusive() {
        return this.maxExclusive().add(-1,-1,-1);
    }
    public Vec3i size() {
        return this.size;
    }
    public int totalBlocks() {
        return this.size.x * this.size.y * this.size.z;
    }

    public boolean contains(Vec3i pos) {
        if (
                pos.x < min.x ||
                        pos.y < min.y ||
                        pos.z < min.z
        ) {
            return false;
        }

        Vec3i maxEx = this.maxExclusive();
        if (
                pos.x >= maxEx.x ||
                        pos.y >= maxEx.y ||
                        pos.z >= maxEx.z
        ) {
            return false;
        }

        return true;
    }

    @Override
    public Iterator<Vec3i> iterator() {
        return new BlockRangeIterator();
    }

    private class BlockRangeIterator implements Iterator<Vec3i> {
        private int maxX = maxExclusive().x;
        private int maxY = maxExclusive().y;
        private int maxZ = maxExclusive().z;

        private int nextX = min.x;
        private int nextY = min.y;
        private int nextZ = min.z;

        @Override
        public boolean hasNext() {
            return nextX < maxX && nextY < maxY && nextZ < maxZ;
        }

        @Override
        public Vec3i next() {
            if (!hasNext())
                throw new NoSuchElementException();

            Vec3i currentPos = new Vec3i(nextX, nextY, nextZ);

            nextZ++;
            if (nextZ == maxZ) {
                nextZ = min.z;
                nextY++;
                if (nextY == maxY) {
                    nextY = min.y;
                    nextX++;
                }
            }

            return currentPos;
        }
    }

    public BlockRange shift(Vec3i offset) {
        return new BlockRange(this.min.add(offset), this.size);
    }

    public BlockRange expand(Vec3i radius) {
        return new BlockRange(this.min.subtract(radius), this.size.add(radius.multiply(2)));
    }

    public BoundingBox toInclusiveBox() {
        var exclusiveMax = this.maxExclusive();
        return new BoundingBox(
                this.min.x,
                this.min.y,
                this.min.z,

                exclusiveMax.x,
                exclusiveMax.y,
                exclusiveMax.z
        );
    }

    public Vec3i clampWithin(Vec3i pos) {
        var maxInclusive = this.maxInclusive();
        return new Vec3i(
                Math.clamp(pos.x, min.x, maxInclusive.x),
                Math.clamp(pos.y, min.y, maxInclusive.y),
                Math.clamp(pos.z, min.z, maxInclusive.z)
        );
    }

    public Vec3i pickRandom(Random rand) {
        var dx = rand.nextInt(size.x);
        var dy = rand.nextInt(size.y);
        var dz = rand.nextInt(size.z);
        return min.add(dx, dy, dz);
    }
}