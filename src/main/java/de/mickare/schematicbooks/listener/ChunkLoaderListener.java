package de.mickare.schematicbooks.listener;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.data.ChunkPosition;

public class ChunkLoaderListener extends AbstractListener {

  public ChunkLoaderListener(SchematicBooksPlugin plugin) {
    super(plugin);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onChunkLoad(ChunkLoadEvent event) {
    final World world = event.getWorld();
    final ChunkPosition pos = new ChunkPosition(event.getChunk());
    new BukkitRunnable() {
      @Override
      public void run() {
        getPlugin().getEntityManager().getCache(world).getChunk(pos);
      }
    }.runTaskAsynchronously(getPlugin());

  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onChunkLoad(ChunkUnloadEvent event) {
    final World world = event.getWorld();
    final ChunkPosition pos = new ChunkPosition(event.getChunk());
    new BukkitRunnable() {
      @Override
      public void run() {
        getPlugin().getEntityManager().getCache(world).unloadChunk(pos);
      }
    }.runTaskAsynchronously(getPlugin());
  }
}
