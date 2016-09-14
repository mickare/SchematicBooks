package de.mickare.schematicbooks;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import de.mickare.schematicbooks.commands.MainSchematicItemsCommand;
import de.mickare.schematicbooks.data.SqliteSchematicEntityDataStore;
import de.mickare.schematicbooks.data.WorldSchematicEntityCache;
import de.mickare.schematicbooks.data.WorldSchematicEntityStore;
import de.mickare.schematicbooks.listener.ArmorToolListener;
import de.mickare.schematicbooks.listener.ChunkLoaderListener;
import de.mickare.schematicbooks.listener.InteractListener;
import de.mickare.schematicbooks.permission.PermissionCheck;
import de.mickare.schematicbooks.permission.SimplePermissionCheck;
import de.mickare.schematicbooks.permission.WorldGuardPermissionCheck;
import de.mickare.schematicbooks.reflection.ReflectionFunction;
import de.mickare.schematicbooks.reflection.ReflectionPlayerName;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class SchematicBooksPlugin extends JavaPlugin {

  @Getter
  @Setter
  private @NonNull Function<UUID, String> playerNameGetter = (uuid) -> {
    Player player = Bukkit.getPlayer(uuid);
    if (player != null) {
      return player.getName();
    }
    return uuid.toString();
  };

  @Getter
  @Setter
  private @NonNull Function<String, UUID> playerUUIDGetter = (name) -> {
    Player player = Bukkit.getPlayer(name);
    if (player != null) {
      return player.getUniqueId();
    }
    return null;
  };


  private @Getter PermissionCheck permcheck;
  private @Getter EntityManager entityManager;
  private @Getter InfoManager infoManager;
  private @Getter File schematicFolder;

  @Override
  public void onLoad() {
    Interactions.setPlugin(this);
  }

  @Override
  public void onDisable() {

    this.getEntityManager().unloadAll();

    getLogger().info("SchematicItem Plugin disabled!");
  }

  @Override
  public void onEnable() {
    Interactions.setPlugin(this);

    Locale locale = Locale.getDefault();
    ResourceBundle bundle = ResourceBundle.getBundle("Messages", locale);
    Out.setResource(bundle);

    this.schematicFolder = new File(this.getDataFolder(), "schematic");
    if (!this.schematicFolder.isDirectory()) {
      this.schematicFolder.mkdirs();
    }

    setPlayerNameGetter("de.rennschnitzel.cbsuite.main.CB", "getPlayerName");
    setPlayerNameGetter("de.rennschnitzel.cbsuite.main.CB", "getPlayerUUID");

    Plugin worldguard = Bukkit.getPluginManager().getPlugin("WorldGuard");
    if (worldguard != null) {
      this.permcheck = new WorldGuardPermissionCheck();
    } else {
      this.permcheck = new SimplePermissionCheck();
    }

    this.entityManager = new EntityManager(this);
    this.infoManager = new InfoManager(this);

    // Listeners
    if (getServer().getPluginManager().isPluginEnabled("ArmorTools")) {
      new ArmorToolListener(this).register();
    }
    new ChunkLoaderListener(this).register();
    new InteractListener(this).register();


    // Commands
    new MainSchematicItemsCommand(this).register();

    getLogger().info("SchematicItem Plugin enabled!");


    // Preload loaded chunks
    new BukkitRunnable() {
      @Override
      public void run() {
        preloadLoadedChunks();
      }
    }.runTaskLater(this, 1);
  }

  private void preloadLoadedChunks() {
    for (World world : Bukkit.getWorlds()) {
      for (Chunk chunk : world.getLoadedChunks()) {
        SchematicBooksPlugin.this.entityManager.getCache(world).getChunk(chunk);
      }
    }
  }

  public WorldSchematicEntityStore createStoreFor(WorldSchematicEntityCache cache, World world)
      throws Exception {
    return new SqliteSchematicEntityDataStore(cache,
        new File(world.getWorldFolder(), "schematicBookEntities.db"));
  }

  public void setPlayerNameGetter(String className, String methodName) {
    try {
      this.setPlayerNameGetter(ReflectionPlayerName.ofStatic(className, methodName));
    } catch (Exception e) {
      getLogger().log(Level.WARNING,
          "Player name getter " + className + "." + methodName + "(uuid) not found!");
    }
  }

  public String getPlayerName(UUID uuid) {
    if (uuid == null
        || (uuid.getMostSignificantBits() == 0 && uuid.getLeastSignificantBits() == 0)) {
      return "none";
    }
    return this.playerNameGetter.apply(uuid);
  }

  public void setPlayerUUIDGetter(String className, String methodName) {
    try {
      this.setPlayerUUIDGetter(
          ReflectionFunction.ofStatic(className, methodName, UUID.class, String.class).unsafe());
    } catch (Exception e) {
      getLogger().log(Level.WARNING,
          "Player uuid getter " + className + "." + methodName + "(string) not found!");
    }
  }

  public UUID getPlayerUUID(String name) {
    if (name == null || name.equals("none")) {
      return null;
    }
    return this.playerUUIDGetter.apply(name);
  }


}