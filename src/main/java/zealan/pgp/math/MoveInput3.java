package zealan.pgp.math;

import org.bukkit.util.Vector;

public class MoveInput3 {
    public double forward, sideways, up;
    public MoveInput3(double forward, double sideways, double up) {
        this.forward = forward;
        this.sideways = sideways;
        this.up = up;
    }

    public MoveInput3() {
        this(0,0,0);
    }

    public Vector toWorldVector(float yaw) {

        var yawRad = Math.toRadians(yaw);
        var sy = Math.sin(yawRad);
        var cy = Math.cos(yawRad);

        var mx = (sideways * cy) - (forward * sy);
        var mz = (forward * cy) + (sideways * sy);

        return new Vector(mx, up, mz);
    }
}
