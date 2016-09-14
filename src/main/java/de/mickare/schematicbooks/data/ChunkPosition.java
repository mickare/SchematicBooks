package de.mickare.schematicbooks.data;

import java.util.Objects;

import org.bukkit.Chunk;

import de.mickare.schematicbooks.util.IntRegion;
import de.mickare.schematicbooks.util.IntVector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


public @RequiredArgsConstructor final class ChunkPosition {

  public static ChunkPosition of(final int x, final int z) {
    return new ChunkPosition(x, z);
  }

  private @Getter final int x, z;

  public ChunkPosition(Chunk chunk) {
    this(chunk.getX(), chunk.getZ());
  }

  public boolean intersects(final IntRegion box) {
    final IntVector start = getMinPoint();
    final IntVector end = getMaxPoint();

    final IntVector min = box.getMinPoint();
    final IntVector max = box.getMaxPoint();

    return min.getX() <= end.getX() && min.getZ() <= end.getZ() //
        && max.getX() >= start.getX() && max.getZ() >= start.getZ();
  }

  public boolean intersects(final IntVector pos1, final IntVector pos2) {
    return intersects(new IntRegion(pos1, pos2));
  }

  @Override
  public final int hashCode() {
    return Objects.hash(x, z);
  }

  public final boolean isSame(Chunk chunk) {
    return isSame(chunk.getX(), chunk.getZ());
  }

  public final boolean isSame(final int x, final int z) {
    return x == this.x && z == this.z;
  }

  public final boolean equals(final ChunkPosition other) {
    return isSame(other.x, other.z);
  }

  @Override
  public final boolean equals(final Object obj) {
    if (obj instanceof ChunkPosition) {
      return equals((ChunkPosition) obj);
    }
    return false;
  }

  /**
   * Inclusive
   * 
   * @return minimal position
   */
  public IntVector getMinPoint() {
    return new IntVector(x << 4, 0, z << 4);
  }


  /**
   * Inclusive
   * 
   * @return max position inclusive
   */
  public IntVector getMaxPoint() {
    return getMinPoint().add(15, 256, 15);
  }

  @Override
  public String toString() {
    return "ChunkPosition[" + x + "," + z + "]";
  }


}
