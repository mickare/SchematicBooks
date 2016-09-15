package de.mickare.schematicbooks.data;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.util.UnsafeBiFunction;
import lombok.Getter;

public class WorldSchematicEntityCache {

  private final JavaPlugin plugin;
  private @Getter final String world;
  private @Getter final File folder;
  private final WorldSchematicEntityStore store;

  private final LoadingCache<Long, SchematicEntity> entities_cache = CacheBuilder.newBuilder()//
      .weakValues()//
      .build(new CacheLoader<Long, SchematicEntity>() {
        @Override
        public SchematicEntity load(Long entityId) throws Exception {
          return new SchematicEntity(entityId);
        }
      });

  private final LoadingCache<ChunkPosition, SchematicChunk> chunks = CacheBuilder.newBuilder()//
      .removalListener(new RemovalListener<ChunkPosition, SchematicChunk>() {
        @Override
        public void onRemoval(RemovalNotification<ChunkPosition, SchematicChunk> notification) {
          WorldSchematicEntityCache.this.saveIfDirty(notification.getValue());
        }
      })//
      .build(new CacheLoader<ChunkPosition, SchematicChunk>() {
        @Override
        public SchematicChunk load(ChunkPosition pos) throws Exception {
          SchematicChunk chunk = new SchematicChunk(WorldSchematicEntityCache.this, pos);
          chunk.load(store);
          return chunk;
        }
      });

  public WorldSchematicEntityCache(SchematicBooksPlugin plugin, World world,
      UnsafeBiFunction<WorldSchematicEntityCache, World, WorldSchematicEntityStore> storeConstructor)
      throws Exception {
    Preconditions.checkNotNull(plugin);
    Preconditions.checkNotNull(world);
    this.plugin = plugin;
    this.world = world.getName();
    this.folder = world.getWorldFolder();
    this.store = storeConstructor.apply(this, world);
  }

  protected synchronized SchematicEntity computeIfAbsent(long id) {
    SchematicEntity entity = entities_cache.getUnchecked(id);
    if (!entity.isValid()) {
      entities_cache.refresh(id);
      return entities_cache.getUnchecked(id);
    }
    return entity;
  }

  public void add(final SchematicEntity entity) throws DataStoreException {
    Preconditions.checkArgument(entity.isValid());
    entity.saveIfDirty(store);
    entity.getHitBox().getChunks().forEach(pos -> getChunk(pos).addEntity(entity));
    entities_cache.put(entity.getId(), entity);
  }

  public void remove(SchematicEntity entity) throws DataStoreException {

    final AtomicBoolean removed = new AtomicBoolean(false);
    entity.getHitBox().getChunks()
        .forEach(pos -> removed.compareAndSet(false, getChunk(pos).removeEntity(entity)));

    if (entity.isValid() || removed.get()) {
      entity.invalidate();
      if (entity.hasId()) {
        store.remove(Collections.singletonList(entity.getId()));
      }
      entities_cache.invalidate(entity.getId());
    }
  }

  public SchematicChunk getChunk(Location loc) {
    return getChunk(loc.getChunk());
  }

  public SchematicChunk getChunk(Chunk chunk) {
    return getChunk(chunk.getX(), chunk.getZ());
  }

  public SchematicChunk getChunk(int x, int z) {
    return getChunk(new ChunkPosition(x, z));
  }

  public SchematicChunk getChunk(ChunkPosition pos) {
    Preconditions.checkNotNull(pos);
    return chunks.getUnchecked(pos);
  }

  public void unloadChunk(Chunk chunk) {
    unloadChunk(chunk.getX(), chunk.getZ());
  }

  public void unloadChunk(int x, int z) {
    unloadChunk(new ChunkPosition(x, z));
  }

  public void unloadChunk(ChunkPosition pos) {
    Preconditions.checkNotNull(pos);
    chunks.invalidate(pos);
  }

  public void unloadAll() {
    chunks.invalidateAll();
  }

  private void saveIfDirty(SchematicChunk chunk) {
    try {
      chunk.saveIfDirty(store);
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to save ItemChunk", e);
    }
  }

  public void saveAll() {
    this.chunks.asMap().values().forEach(this::saveIfDirty);
  }

  public Optional<SchematicEntity> getEntityOf(Entity entity) {
    return getEntityOf(entity.getUniqueId(), entity.getLocation());
  }

  public Optional<SchematicEntity> getEntityOf(UUID uuid, Location location) {
    return getChunk(location).getEntityOf(uuid);
  }

  public Set<SchematicEntity> getEntitiesAt(Location loc) {
    return getChunk(loc).getEntitiesAt(loc);
  }

  public Set<SchematicEntity> getEntitiesAt(Block block) {
    return getChunk(block.getChunk()).getEntitiesAt(block.getX(), block.getY(), block.getZ());
  }


  public SchematicEntity getEntity(long id) throws DataStoreException {
    SchematicEntity entity = this.entities_cache.getIfPresent(id);
    if (entity != null && entity.isValid()) {
      return entity;
    }
    return this.store.load(id);
  }


}
