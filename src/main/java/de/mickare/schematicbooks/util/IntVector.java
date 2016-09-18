package de.mickare.schematicbooks.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class IntVector {

  public static IntVector from(Vector vec) {
    return new IntVector(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
  }

  public static IntVector from(com.sk89q.worldedit.Vector vec) {
    return new IntVector(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
  }

  public static IntVector from(Location loc) {
    return new IntVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  private @Getter @Setter int x, y, z;

  public IntVector() {
    this(0, 0, 0);
  }

  public Vector toVector() {
    return new Vector(x, y, z);
  }

  public IntVector copy() {
    return new IntVector(x, y, z);
  }

  public IntVector copy(Vector vec) {
    this.x = vec.getBlockX();
    this.y = vec.getBlockY();
    this.z = vec.getBlockZ();
    return this;
  }

  public IntVector copy(IntVector vec) {
    this.x = vec.x;
    this.y = vec.y;
    this.z = vec.z;
    return this;
  }

  public int getChunkX() {
    return x >> 4;
  }

  public int getChunkZ() {
    return z >> 4;
  }

  public IntVector max(IntVector vec) {
    return new IntVector(//
        Math.max(x, vec.x), //
        Math.max(y, vec.y), //
        Math.max(z, vec.z));
  }

  public IntVector min(IntVector vec) {
    return new IntVector(//
        Math.min(x, vec.x), //
        Math.min(y, vec.y), //
        Math.min(z, vec.z));
  }


  public IntVector add(IntVector vec) {
    return add(vec.x, vec.y, vec.z);
  }

  public IntVector add(int x, int y, int z) {
    this.x += x;
    this.y += y;
    this.z += z;
    return this;
  }

  public IntVector subtract(IntVector vec) {
    return subtract(vec.x, vec.y, vec.z);
  }

  public IntVector subtract(int x, int y, int z) {
    this.x -= x;
    this.y -= y;
    this.z -= z;
    return this;
  }

  public Location setLocation(Location loc) {
    loc.setX(x);
    loc.setY(y);
    loc.setZ(z);
    return loc;
  }

  public Location toLocation(World world) {
    return new Location(world, this.x, this.y, this.z);
  }

  public String toString() {
    return "(" + x + "," + y + "," + z + ")";
  }

  @Override
  public int hashCode() {
    return x * 31 * 31 + z * 31 + y;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof IntVector) {
      IntVector other = (IntVector) obj;
      return other.getX() == this.x && other.getY() == this.y && other.getZ() == this.z;
    }
    return false;
  }


}
