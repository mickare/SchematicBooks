package de.mickare.schematicbooks.data;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.World;
import org.bukkit.entity.Entity;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import de.mickare.schematicbooks.SchematicBookInfo;
import de.mickare.schematicbooks.util.IntRegion;
import de.mickare.schematicbooks.util.IntVector;
import de.mickare.schematicbooks.util.Rotation;
import lombok.Getter;

public class SchematicEntity {

  @FunctionalInterface
  public static interface Setter {
    void set(String name, Rotation rotation, IntVector start, IntVector end, Set<UUID> entities,
        long timestamp) throws IllegalArgumentException, NullPointerException;
  }

  private transient @Getter boolean valid = true;
  private transient @Getter boolean dirty = false;

  private Long id = null;

  private @Getter String name;
  private @Getter Set<UUID> entities;
  private @Getter Rotation rotation;

  private @Getter IntRegion hitBox;
  private @Getter long timestamp;
  private @Getter UUID owner;

  protected SchematicEntity(long id) {
    this.id = id;
  }

  public String getKey() {
    return SchematicBookInfo.makeKey(name);
  }

  public SchematicEntity(String name, Rotation rotation, IntVector start, IntVector end,
      Set<UUID> entities, UUID owner) {
    set(name, rotation, start, end, entities, System.currentTimeMillis(), owner);
    this.dirty = true;
  }

  public final boolean hasId() {
    return id != null;
  }

  public final long getId() {
    Preconditions.checkState(id != null, "has no ID");
    return id.longValue();
  }

  protected void set(String name, Rotation rotation, IntVector start, IntVector end,
      Set<UUID> entities, long timestamp, UUID owner)
      throws IllegalArgumentException, NullPointerException {
    Preconditions.checkArgument(name.length() > 0);
    Preconditions.checkNotNull(rotation);
    Preconditions.checkNotNull(entities);
    Preconditions.checkNotNull(owner);
    this.name = name;
    this.rotation = rotation;
    this.hitBox = new IntRegion(start, end);
    this.entities = ImmutableSet.copyOf(entities);
    this.timestamp = timestamp;
    this.owner = owner;

    this.dirty = false;
  }



  public boolean hasEntity(Entity entity) {
    return this.hasEntity(entity.getUniqueId());
  }

  public boolean hasEntity(UUID entityID) {
    return entities.contains(entityID);
  }

  public Stream<Entity> getEntities(World world, int additional) {
    return hitBox.getChunks(additional).stream()//
        .map(pos -> world.getChunkAt(pos.getX(), pos.getZ()))//
        .filter(c -> c.isLoaded() ? true : c.load(false))//
        .flatMap(c -> Arrays.stream(c.getEntities()))//
        .filter(e -> hasEntity(e.getUniqueId()))//
    ;
  }

  public void save(WorldSchematicEntityStore store) throws DataStoreException {
    if (valid) {
      id = store.save(this);
      this.dirty = false;
    }
  }

  public synchronized void saveIfDirty(WorldSchematicEntityStore store) throws DataStoreException {
    if (dirty) {
      this.save(store);
    }
  }

  public void invalidate() {
    this.valid = false;
  }

}
