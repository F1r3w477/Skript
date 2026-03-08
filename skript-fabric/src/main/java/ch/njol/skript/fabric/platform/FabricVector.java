package ch.njol.skript.fabric.platform;

import java.util.Objects;

/**
 * Simple 3D vector for Fabric script expressions (vector between, vector from xyz, etc.).
 * Equality uses a small tolerance for double comparison.
 */
public final class FabricVector {

    private static final double EPSILON = 1e-9;

    private final double x;
    private final double y;
    private final double z;

    public FabricVector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FabricVector that = (FabricVector) o;
        return Math.abs(that.x - x) < EPSILON
            && Math.abs(that.y - y) < EPSILON
            && Math.abs(that.z - z) < EPSILON;
    }

    @Override
    public int hashCode() {
        return Objects.hash((long)(x / EPSILON), (long)(y / EPSILON), (long)(z / EPSILON));
    }

    @Override
    public String toString() {
        return "vector(" + x + "," + y + "," + z + ")";
    }
}
