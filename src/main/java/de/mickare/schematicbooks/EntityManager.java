package de.mickare.schematicbooks;

import java.util.Optional;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import de.mickare.schematicbooks.data.SchematicEntity;
import de.mickare.schematicbooks.data.WorldSchematicEntityCache;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityManager {

  private @NonNull final SchematicBooksPlugin plugin;

  private LoadingCache<World, WorldSchematicEntityCache> worlds = CacheBuilder.newBuilder()//
      .weakKeys()//
      .removalListener(new RemovalListener<World, WorldSchematicEntityCache>() {
        @Override
        public void onRemoval(RemovalNotification<World, WorldSchematicEntityCache> notification) {
          if (notification.getKey().getWorldFolder().exists()) {
            notification.getValue().saveAll();
          }
        }
      })//
      .build(new CacheLoader<World, WorldSchematicEntityCache>() {
        @Override
        public WorldSchematicEntityCache load(World world) throws Exception {
          return new WorldSchematicEntityCache(plugin, world, plugin::createStoreFor);
        }
      });

  public Optional<SchematicEntity> getEntityOf(final Entity entity) {
    return getCache(entity.getWorld()).getEntityOf(entity);
  }

  public Set<SchematicEntity> getEntitiesAt(Location loc) {
    return getCache(loc.getWorld()).getEntitiesAt(loc);
  }

  public Set<SchematicEntity> getEntitiesAt(Block block) {
    return getCache(block.getWorld()).getEntitiesAt(block);
  }

  public WorldSchematicEntityCache getCache(final World world) {
    return worlds.getUnchecked(world);
  }

  public void saveAll() {
    worlds.asMap().values().forEach(WorldSchematicEntityCache::saveAll);
  }

  public void invalidateAll() {
    this.worlds.invalidateAll();
  }

  public void unloadAll() {
    this.invalidateAll();
  }


}
