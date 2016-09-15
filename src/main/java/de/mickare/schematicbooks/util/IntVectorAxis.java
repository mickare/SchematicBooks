package de.mickare.schematicbooks.util;

public class IntVectorAxis {

  private final IntVector negativeAxis;
  private final IntVector positiveAxis;


  public IntVectorAxis() {
    this.negativeAxis = new IntVector();
    this.positiveAxis = new IntVector();
  }

  public IntVectorAxis(IntVector negativeAxis, IntVector positiveAxis) {
    this.negativeAxis = negativeAxis.copy();
    this.positiveAxis = positiveAxis.copy();
  }

  public IntRegion addTo(IntRegion box) {
    return new IntRegion(box.getMinPoint().add(negativeAxis), box.getMaxPoint().add(positiveAxis));
  }

  public IntVector getNegativeAxis() {
    return negativeAxis.copy();
  }

  public IntVector getPositiveAxis() {
    return positiveAxis.copy();
  }

  public IntVectorAxis copy() {
    return new IntVectorAxis(negativeAxis, positiveAxis);
  }

  public IntVectorAxis rotate(Rotation rotation) {
    return rotate(rotation.getYaw());
  }

  public IntVectorAxis rotate(int yaw) {
    float myaw = yaw % 360 + (yaw < 0 ? 360 : 0);

    if (myaw == 0) {
      return this.copy();
    } else if (myaw == 90) {
      IntVector rmin =
          new IntVector(-positiveAxis.getZ(), negativeAxis.getY(), negativeAxis.getX()); // 1|0
      IntVector rmax =
          new IntVector(-negativeAxis.getZ(), positiveAxis.getY(), positiveAxis.getX()); // 0|1
      return new IntVectorAxis(rmin, rmax);
    } else if (myaw == 180) {
      IntVector rmin =
          new IntVector(-positiveAxis.getZ(), negativeAxis.getY(), -positiveAxis.getX()); // 1|1
      IntVector rmax =
          new IntVector(-negativeAxis.getZ(), positiveAxis.getY(), -negativeAxis.getX()); // 0|0
      return new IntVectorAxis(rmin, rmax);
    } else if (myaw == 270) {
      IntVector rmin =
          new IntVector(negativeAxis.getZ(), negativeAxis.getY(), -positiveAxis.getX()); // 0|1
      IntVector rmax =
          new IntVector(positiveAxis.getZ(), positiveAxis.getY(), -negativeAxis.getX()); // 1|0
      return new IntVectorAxis(rmin, rmax);
    }
    throw new IllegalArgumentException("Unsupported yaw " + yaw);
  }

  public IntVectorAxis expand(IntVector vec) {
    if (vec.getX() > 0) {
      positiveAxis.add(vec.getX(), 0, 0);
    } else {
      negativeAxis.add(vec.getX(), 0, 0);
    }
    if (vec.getY() > 0) {
      positiveAxis.add(0, vec.getY(), 0);
    } else {
      negativeAxis.add(0, vec.getY(), 0);
    }
    if (vec.getZ() > 0) {
      positiveAxis.add(0, 0, vec.getZ());
    } else {
      negativeAxis.add(0, 0, vec.getZ());
    }
    return this;
  }

  public IntVectorAxis contract(IntVector vec) {
    if (vec.getX() < 0) {
      positiveAxis.add(vec.getX(), 0, 0);
    } else {
      negativeAxis.add(vec.getX(), 0, 0);
    }
    if (vec.getY() < 0) {
      positiveAxis.add(0, vec.getY(), 0);
    } else {
      negativeAxis.add(0, vec.getY(), 0);
    }
    if (vec.getZ() < 0) {
      positiveAxis.add(0, 0, vec.getZ());
    } else {
      negativeAxis.add(0, 0, vec.getZ());
    }
    return this;
  }

  public IntVectorAxis subtract(IntVectorAxis other) {
    IntVector negDiff = this.getNegativeAxis().subtract(other.getNegativeAxis());
    IntVector posDiff = this.getPositiveAxis().subtract(other.getPositiveAxis());
    return new IntVectorAxis(negDiff, posDiff);
  }

  @Override
  public String toString() {
    return "(NegativeAxis:" + this.negativeAxis.toString() + ", PositiveAxis:"
        + this.positiveAxis.toString() + ")";
  }

}
