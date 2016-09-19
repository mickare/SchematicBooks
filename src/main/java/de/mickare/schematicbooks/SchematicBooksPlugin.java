package de.mickare.schematicbooks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import de.mickare.schematicbooks.listener.WorldEditListener;
import de.mickare.schematicbooks.permission.PermissionCheck;
import de.mickare.schematicbooks.permission.SimplePermissionCheck;
import de.mickare.schematicbooks.permission.WorldGuardPermissionCheck;
import de.mickare.schematicbooks.reflection.ReflectionPlayerInformation;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class SchematicBooksPlugin extends JavaPlugin {

  // Normal Entities that are part of Schematic Entities.
  private static final int ENTITY_LIMIT_PER_CHUNK = 500;

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
  private @Getter Path schematicFolder;

  @Override
  public void onLoad() {
    Interactions.setPlugin(this);
  }

  @Override
  public void onDisable() {
    if (this.infoManager != null) {
      try {
        this.infoManager.close();
      } catch (IOException e) {
        getLogger().log(Level.WARNING, "Could not close InfoManager.", e);
      }
    }

    if (this.entityManager != null) {
      this.entityManager.unloadAll();
    }

    getLogger().info("SchematicItem Plugin disabled!");
  }

  @Override
  public void onEnable() {
    Interactions.setPlugin(this);

    Locale locale = Locale.getDefault();
    ResourceBundle bundle = ResourceBundle.getBundle("Messages", locale);
    Out.setResource(bundle);

    this.schematicFolder = this.getDataFolder().toPath().resolve("schematic");
    if (!Files.isDirectory(this.schematicFolder)) {
      try {
        Files.createDirectories(this.schematicFolder);
      } catch (IOException e) {
        throw new RuntimeException("Could not create schematic folder!" + e);
      }
    }
    getLogger().info("Schematic Folder: " + this.schematicFolder.toString());

    setPlayerNameGetter("de.rennschnitzel.cbsuite.main.CB", "getPlayerName");
    setPlayerUUIDGetter("de.rennschnitzel.cbsuite.main.CB", "getPlayerUUID");

    Plugin worldguard = Bukkit.getPluginManager().getPlugin("WorldGuard");
    if (worldguard != null) {
      this.permcheck = new WorldGuardPermissionCheck();
    } else {
      this.permcheck = new SimplePermissionCheck();
    }

    try {
      this.entityManager = new EntityManager(this);
      this.infoManager = new InfoManager(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Listeners
    Plugin armortools = Bukkit.getPluginManager().getPlugin("ArmorTools");
    if (armortools != null) {
      new ArmorToolListener(this).register();
    }
    new ChunkLoaderListener(this).register();
    new InteractListener(this).register();
    new WorldEditListener(this).register();


    // Commands
    new MainSchematicItemsCommand(this).register();

    getLogger().info("SchematicItem Plugin enabled!");

    doPreload(false);
  }

  public int getEntityLimitPerChunk(Player player) {
    return ENTITY_LIMIT_PER_CHUNK;
  }

  private void doPreload(boolean later) {
    if (later) {
      // Preload loaded chunks
      new BukkitRunnable() {
        @Override
        public void run() {
          getLogger().info("Preloading...");
          loadLoadedChunks();
          getInfoManager().loadAllInfoFiles();
        }
      }.runTaskLater(this, 3);
    } else {
      getLogger().info("Preloading...");
      loadLoadedChunks();
      getInfoManager().loadAllInfoFiles();
    }
  }

  public void loadLoadedChunks() {
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
      this.setPlayerNameGetter(ReflectionPlayerInformation.nameByStatic(className, methodName));
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
      this.setPlayerUUIDGetter(ReflectionPlayerInformation.uuidByStatic(className, methodName));
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
