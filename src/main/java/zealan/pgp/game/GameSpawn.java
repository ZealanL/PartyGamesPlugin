package zealan.pgp.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import zealan.pgp.math.Vec3i;

public class GameSpawn {
    public final Vector pos;
    public final BlockFace dir;

    public GameSpawn(Vector spawnPos) {
        this.pos = spawnPos;
        this.dir = BlockFace.NORTH;
    }
    public GameSpawn(Vector spawnPos, BlockFace spawnDir) {
        this.pos = spawnPos;
        this.dir = spawnDir;
    }

    // ////////

    public GameSpawn(Vec3i spawnPos) {
        this(spawnPos.getBottomCenter());
    }

    public GameSpawn(Vec3i spawnPos, BlockFace spawnDir) {
        this(spawnPos.getBottomCenter(), spawnDir);
    }

    public GameSpawn(int spawnX, int spawnY, int spawnZ, BlockFace spawnDir) {
        this(new Vec3i(spawnX, spawnY, spawnZ), spawnDir);
    }

    public GameSpawn(int spawnX, int spawnY, int spawnZ) {
        this(new Vec3i(spawnX, spawnY, spawnZ));
    }

    // ////////

    public Vec3i getBlockPos() {
        return Vec3i.from(pos);
    }
    public BlockFace getDir() {
        return dir;
    }

    public Location toLocation(World world) {
        return new Location(world, this.pos.getX(), this.pos.getY(), this.pos.getZ());
    }
}
