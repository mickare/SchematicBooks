package de.mickare.schematicbooks.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;

import com.google.common.base.Preconditions;

import de.mickare.schematicbooks.SchematicBooksPlugin;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocationBlockIterator<T> implements Iterator<T> {

  private @NonNull final BlockIterator blockIterator;
  private @NonNull final Function<Location, Iterator<T>> dataGetter;
  private @NonNull final Predicate<Material> blockTypeAbort;

  private final Location location = new Location(null, 0, 0, 0);
  private Material blockType = null;
  private Iterator<T> current = null;

  public static <T> LocationBlockIterator<T> iterable(final BlockIterator blockIterator,
      final Function<Location, Iterable<T>> dataGetter) {
    return new LocationBlockIterator<T>(blockIterator, (loc) -> dataGetter.apply(loc).iterator());
  }

  public static <T> LocationBlockIterator<T> iterable(final BlockIterator blockIterator,
      final Function<Location, Iterable<T>> dataGetter, Predicate<Material> blockTypeAbort) {
    return new LocationBlockIterator<T>(blockIterator, (loc) -> dataGetter.apply(loc).iterator(),
        blockTypeAbort);
  }

  public LocationBlockIterator(final BlockIterator blockIterator,
      final Function<Location, Iterator<T>> dataGetter) {
    this(blockIterator, dataGetter, (t) -> (t != null ? t.isSolid() : false));
  }

  public World getWorld() {
    return location.getWorld();
  }

  public Location getLocation() {
    return location.clone();
  }

  public boolean hasNextBlock() {
    if (blockTypeAbort.test(blockType)) {
      return false;
    }
    return blockIterator.hasNext();
  }

  public Block nextBlock() {
    Block block = blockIterator.next();
    this.blockType = block.getType();
    block.getLocation(location);
    current = dataGetter.apply(location);
    Preconditions.checkNotNull(current);
    return block;
  }

  @Override
  public boolean hasNext() {
    boolean hasNext = current != null ? current.hasNext() : false;
    while (!hasNext && hasNextBlock()) {
      nextBlock();
      hasNext = current.hasNext();
    }
    return hasNext;
  }

  @Override
  public T next() {
    if (!this.hasNext()) { // automatically advances the block iterator
      throw new NoSuchElementException();
    }
    return current.next();
  }

}
