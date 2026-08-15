package zealan.pgp.math;

import java.util.ArrayList;
import java.util.List;

public class BlockRange2d {
    public final int minX, minZ, maxX, maxZ;
    public BlockRange2d(int x1, int z1, int x2, int z2) {
        this.minX = Math.min(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxZ = Math.max(z1, z2);
    }

    private static int blockToRegion(int blockCoord) {
        return blockCoord >> 9;
    }

    public List<String> getRegionFilenames() {
        List<String> results = new ArrayList<>();

        int minRegionX = this.minX >> 9;
        int maxRegionX = this.maxX >> 9;
        int minRegionZ = this.minZ >> 9;
        int maxRegionZ = this.maxZ >> 9;

        for (int rx = minRegionX; rx <= maxRegionX; rx++)
            for (int rz = minRegionZ; rz <= maxRegionZ; rz++)
                results.add(String.format("r.%d.%d.mca", rx, rz));
        return results;
    }
}
