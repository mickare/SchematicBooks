package de.mickare.schematicbooks.util;

public class IntVectorTuple {

  private final IntVector min;
  private final IntVector max;


  public IntVectorTuple() {
    this.min = new IntVector();
    this.max = new IntVector();
  }

  public IntVectorTuple(IntVector min, IntVector max) {
    this.min = min.copy();
    this.max = max.copy();
  }

  public IntRegion addTo(IntRegion box) {
    return new IntRegion(box.getMinPoint().add(min), box.getMaxPoint().add(max));
  }

  public IntVector getMinPoint() {
    return min.copy();
  }

  public IntVector getMaxPoint() {
    return max.copy();
  }

  public IntVectorTuple copy() {
    return new IntVectorTuple(min, max);
  }

  public IntVectorTuple rotate(Rotation rotation) {
    return rotate(rotation.getYaw());
  }

  public IntVectorTuple rotate(int yaw) {
    float myaw = yaw % 360 + (yaw < 0 ? 360 : 0);

    if (myaw == 0) {
      return this.copy();
    } else if (myaw == 90) {
      IntVector rmin = new IntVector(-max.getZ(), min.getY(), min.getX()); // 1|0
      IntVector rmax = new IntVector(-min.getZ(), max.getY(), max.getX()); // 0|1
      return new IntVectorTuple(rmin, rmax);
    } else if (myaw == 180) {
      IntVector rmin = new IntVector(-max.getZ(), min.getY(), -max.getX()); // 1|1
      IntVector rmax = new IntVector(-min.getZ(), max.getY(), -min.getX()); // 0|0
      return new IntVectorTuple(rmin, rmax);
    } else if (myaw == 270) {
      IntVector rmin = new IntVector(min.getZ(), min.getY(), -max.getX()); // 0|1
      IntVector rmax = new IntVector(max.getZ(), max.getY(), -min.getX()); // 1|0
      return new IntVectorTuple(rmin, rmax);
    }
    throw new IllegalArgumentException("Unsupported yaw " + yaw);
  }

  public IntVectorTuple expand(IntVector vec) {
    if (vec.getX() > 0) {
      max.add(vec.getX(), 0, 0);
    } else {
      min.add(vec.getX(), 0, 0);
    }
    if (vec.getY() > 0) {
      max.add(0, vec.getY(), 0);
    } else {
      min.add(0, vec.getY(), 0);
    }
    if (vec.getZ() > 0) {
      max.add(0, 0, vec.getZ());
    } else {
      min.add(0, 0, vec.getZ());
    }
    return this;
  }

  public IntVectorTuple contract(IntVector vec) {
    if (vec.getX() < 0) {
      max.add(vec.getX(), 0, 0);
    } else {
      min.add(vec.getX(), 0, 0);
    }
    if (vec.getY() < 0) {
      max.add(0, vec.getY(), 0);
    } else {
      min.add(0, vec.getY(), 0);
    }
    if (vec.getZ() < 0) {
      max.add(0, 0, vec.getZ());
    } else {
      min.add(0, 0, vec.getZ());
    }
    return this;
  }

}
