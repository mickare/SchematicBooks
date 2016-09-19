package de.mickare.schematicbooks.util;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import com.google.common.collect.Sets;

import de.mickare.schematicbooks.data.ChunkPosition;
import lombok.Getter;
import lombok.NonNull;

public class IntRegion {

  @Getter
  private @NonNull final IntVector pos1, pos2;


  public IntRegion(Location start, int x, int y, int z) {
    this(new IntVector(start.getBlockX(), start.getBlockY(), start.getBlockZ()), x, y, z);
  }

  public IntRegion(IntVector start, int x, int y, int z) {
    this(start, start.copy().add(x, y, z));
  }


  public IntRegion(IntRegion other) {
    this(other.getPos1(), other.getPos2());
  }

  public IntRegion(IntVector first, IntVector second) {
    this.pos1 = first.min(second);
    this.pos2 = first.max(second);
  }

  public IntRegion copy() {
    return new IntRegion(this);
  }

  public IntVector getMinPoint() {
    return pos1.min(pos2);
  }

  public IntVector getMaxPoint() {
    return pos1.max(pos2);
  }

  public boolean intersects(Location loc) {
    return intersects(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  public boolean intersects(Vector vec) {
    return intersects(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
  }

  public boolean intersects(final int x, final int y, final int z) {
    final IntVector min = getMinPoint();
    final IntVector max = getMaxPoint();

    return min.getX() <= x && x <= max.getX() //
        && min.getY() <= y && y <= max.getY() //
        && min.getZ() <= z && z <= max.getZ();
  }

  public boolean intersects(IntRegion region) {
    return region.getMinPoint().compareValues(this.getMaxPoint(), (x, y) -> x <= y)//
        && region.getMaxPoint().compareValues(this.getMinPoint(), (x, y) -> x >= y);
  }

  public boolean contains(IntRegion region) {
    return region.getMinPoint().compareValues(this.getMinPoint(), (x, y) -> x >= y)//
        && region.getMaxPoint().compareValues(this.getMaxPoint(), (x, y) -> x <= y);
  }

  public IntVector size() {
    return pos1.copy().subtract(pos2).add(1, 1, 1);
  }

  public Set<ChunkPosition> getChunks() {
    final IntVector min = getMinPoint();
    final IntVector max = getMaxPoint();

    Set<ChunkPosition> positions = Sets.newHashSet();
    for (int chunkX = min.getChunkX(); chunkX <= max.getChunkX(); ++chunkX) {
      for (int chunkZ = min.getChunkZ(); chunkZ <= max.getChunkZ(); ++chunkZ) {
        positions.add(ChunkPosition.of(chunkX, chunkZ));
      }
    }
    return positions;
  }

  public Set<ChunkPosition> getChunks(int additional) {
    final IntVector min = getMinPoint();
    final IntVector max = getMaxPoint();

    Set<ChunkPosition> positions = Sets.newHashSet();
    int chunkX = min.getChunkX() - additional;
    int chunkZ = min.getChunkZ() - additional;
    for (; chunkX <= max.getChunkX() + additional; ++chunkX) {
      for (; chunkZ <= max.getChunkZ() + additional; ++chunkZ) {
        positions.add(ChunkPosition.of(chunkX, chunkZ));
      }
    }
    return positions;
  }

  public Stream<IntVector> positions() {
    final IntVector min = getMinPoint();
    final IntVector max = getMaxPoint();

    Stream.Builder<IntVector> b = Stream.builder();
    for (int x = min.getX(); x <= max.getX(); x++) {
      for (int y = min.getY(); y <= max.getY(); y++) {
        for (int z = min.getZ(); z <= max.getZ(); z++) {
          b.accept(new IntVector(x, y, z));
        }
      }
    }
    return b.build();
  }

  public String toString() {
    return "(min:" + this.getMinPoint().toString() + ", max:" + this.getMaxPoint().toString() + ")";
  }

  private IntVector getMinVector(Function<IntVector, Integer> getter) {
    return getter.apply(pos1) <= getter.apply(pos2) ? pos1 : pos2;
  }

  private IntVector getMaxVector(Function<IntVector, Integer> getter) {
    return getter.apply(pos1) >= getter.apply(pos2) ? pos1 : pos2;
  }

  public IntRegion expand(IntVector vector) {

    if (vector.getX() > 0) {
      getMaxVector(IntVector::getX).add(vector.getX(), 0, 0);
    } else {
      getMinVector(IntVector::getX).add(vector.getX(), 0, 0);
    }

    if (vector.getY() > 0) {
      getMaxVector(IntVector::getY).add(0, vector.getY(), 0);
    } else {
      getMinVector(IntVector::getY).add(0, vector.getY(), 0);
    }

    if (vector.getZ() > 0) {
      getMaxVector(IntVector::getZ).add(0, 0, vector.getZ());
    } else {
      getMinVector(IntVector::getZ).add(0, 0, vector.getZ());
    }

    return this;
  }

  public IntRegion contract(IntVector vector) {

    if (vector.getX() < 0) {
      getMaxVector(IntVector::getX).add(vector.getX(), 0, 0);
    } else {
      getMinVector(IntVector::getX).add(vector.getX(), 0, 0);
    }

    if (vector.getY() < 0) {
      getMaxVector(IntVector::getY).add(0, vector.getY(), 0);
    } else {
      getMinVector(IntVector::getY).add(0, vector.getY(), 0);
    }

    if (vector.getZ() < 0) {
      getMaxVector(IntVector::getZ).add(0, 0, vector.getZ());
    } else {
      getMinVector(IntVector::getZ).add(0, 0, vector.getZ());
    }

    return this;
  }


}
