package de.mickare.schematicbooks.data;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import de.mickare.schematicbooks.util.IntVector;
import lombok.Getter;

public class SchematicChunk {

  private @Getter final WorldSchematicEntityCache cache;
  private @Getter final ChunkPosition position;

  private BiMap<Long, SchematicEntity> entities = HashBiMap.create();

  protected SchematicChunk(WorldSchematicEntityCache cache, ChunkPosition position) {
    Preconditions.checkNotNull(cache);
    Preconditions.checkNotNull(position);
    this.cache = cache;
    this.position = position;
  }

  protected SchematicChunk(WorldSchematicEntityCache cache, int x, int z) {
    this(cache, new ChunkPosition(x, z));
  }

  public int hashCode() {
    return position.hashCode();
  }

  public boolean equals(Object obj) {
    if (obj instanceof SchematicChunk) {
      if (obj == this) {
        return true;
      }
      SchematicChunk other = (SchematicChunk) obj;
      return position.equals(other.position) && entities.equals(other.entities);
    }
    return false;
  }

  public boolean intersects(final SchematicEntity entity) {
    return position.intersects(entity.getHitBox());
  }

  // public int getX() {return position.getX();}
  // public int getZ() {return position.getZ();}

  public BiMap<Long, SchematicEntity> getEntities() {
    return Maps.unmodifiableBiMap(entities);
  }

  protected void addEntity(SchematicEntity entity) throws IllegalArgumentException {
    Preconditions.checkArgument(intersects(entity));
    entities.put(entity.getId(), entity);
  }

  protected SchematicEntity removeEntity(long entityId) {
    return entities.remove(entityId);
  }

  protected boolean removeEntity(SchematicEntity entity) {
    return entities.remove(entity.getId(), entity);
  }

  public SchematicEntity getEntity(long entityId) {
    return entities.get(entityId);
  }

  public Optional<SchematicEntity> getEntityOf(final UUID entityId) {
    return entities.values().stream().filter(g -> g.hasEntity(entityId)).findAny();
  }

  public boolean isEmpty() {
    return entities.isEmpty();
  }

  public boolean isDirty() {
    return entities.values().stream().filter(g -> g.isDirty()).findAny().isPresent();
  }


  public synchronized void saveIfDirty(final WorldSchematicEntityStore store) throws Exception {
    for (SchematicEntity e : entities.values()) {
      e.saveIfDirty(store);
    }
  }

  public synchronized void load(WorldSchematicEntityStore store) throws Exception {
    BiMap<Long, SchematicEntity> newEntities = HashBiMap.create();
    store.load(position).forEach(g -> newEntities.put(g.getId(), g));
    this.entities = newEntities;
  }

  public Set<SchematicEntity> getEntitiesAt(Location loc) {
    return getEntitiesAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  public Set<SchematicEntity> getEntitiesAt(IntVector pos) {
    return getEntitiesAt(pos.getX(), pos.getY(), pos.getZ());
  }

  public Set<SchematicEntity> getEntitiesAt(int x, int y, int z) {
    if (this.entities.isEmpty()) {
      return Collections.emptySet();
    }
    return this.entities.values().stream().filter(g -> g.getHitBox().intersects(x, y, z))
        .collect(Collectors.toSet());
  }

}
