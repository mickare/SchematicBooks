package de.mickare.schematicbooks.data;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import de.mickare.schematicbooks.SchematicBookInfo;
import de.mickare.schematicbooks.util.IntRegion;
import de.mickare.schematicbooks.util.IntVector;
import de.mickare.schematicbooks.util.Rotation;
import lombok.Getter;
import lombok.Setter;

public class SchematicEntity {

  private transient @Getter boolean valid = true;
  private transient @Getter boolean dirty = false;

  private Long id = null;

  private @Getter String name;
  private @Getter Set<UUID> entities;
  private @Getter Rotation rotation;

  private @Getter @Setter IntRegion hitBox;
  private @Getter long timestamp;
  private @Getter UUID owner;

  private @Getter Vector moved;

  private @Getter @Setter boolean movable = false;
  private @Getter @Setter boolean rotatable = false;

  protected SchematicEntity(long id) {
    this.id = id;
  }

  public SchematicEntity dirty() {
    this.dirty = true;
    return this;
  }

  public String getKey() {
    return SchematicBookInfo.makeKey(name);
  }

  public SchematicEntity(String name, Rotation rotation, IntVector start, IntVector end,
      Set<UUID> entities, UUID owner, boolean movable, boolean rotatable) {
    set(name, rotation, start, end, entities, System.currentTimeMillis(), owner, new Vector(),
        movable, rotatable);
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
      Set<UUID> entities, long timestamp, UUID owner, Vector moved, boolean movable,
      boolean rotatable) throws IllegalArgumentException, NullPointerException {
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

    this.moved = moved;
    this.movable = movable;
    this.rotatable = rotatable;

    this.dirty = false;
  }



  public boolean hasEntity(Entity entity) {
    return this.hasEntity(entity.getUniqueId());
  }

  public boolean hasEntity(UUID entityID) {
    return entities.contains(entityID);
  }

  public Stream<Entity> getEntityObjects(World world) {
    if (this.entities.isEmpty()) {
      return Stream.empty();
    }
    Set<UUID> remainingEntities = Sets.newHashSet(this.entities);
    return world.getEntities().stream()//
        .filter(e -> remainingEntities.size() > 0)//
        .filter(e -> remainingEntities.remove(e.getUniqueId()));
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
