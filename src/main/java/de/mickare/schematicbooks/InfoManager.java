package de.mickare.schematicbooks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.inventory.ItemStack;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import de.mickare.schematicbooks.data.SchematicEntity;
import lombok.Getter;

public class InfoManager {

  private @Getter final SchematicBooksPlugin plugin;

  private static Gson GSON = new GsonBuilder().create();
  private LoadingCache<String, SchematicBookInfo> schematics = CacheBuilder.newBuilder()//
      .build(new CacheLoader<String, SchematicBookInfo>() {

        @Override
        public SchematicBookInfo load(String key) throws Exception {
          return loadInfo(getInfoFileOf(key));
        }
      });

  public InfoManager(SchematicBooksPlugin plugin) {
    Preconditions.checkNotNull(plugin);
    this.plugin = plugin;

    File[] files = plugin.getSchematicFolder()
        .listFiles((dir, name) -> name != null && name.endsWith(".info"));
    Preconditions.checkState(files != null);

    for (File infofile : files) {
      try {
        SchematicBookInfo info = loadInfo(infofile);
        schematics.put(info.getKey().toLowerCase(), info);
      } catch (IOException | JsonSyntaxException | JsonIOException e) {
        logger().log(Level.WARNING, "Could not load schematic info " + infofile.getName(), e);
      }
    }

  }

  public void invalidateAll() {
    this.schematics.invalidateAll();
  }

  private Logger logger() {
    return plugin.getLogger();
  }


  public File getInfoFileOf(String key) {
    return new File(plugin.getSchematicFolder(), key.toLowerCase() + ".info");
  }

  public File getInfoFileOf(SchematicBookInfo info) {
    return getInfoFileOf(info.getKey());
  }

  public File getSchematicFileOf(String key) {
    return new File(plugin.getSchematicFolder(), key.toLowerCase() + ".schematic");
  }

  public File getSchematicFileOf(SchematicBookInfo info) {
    return getSchematicFileOf(info.getKey());
  }

  public SchematicBookInfo loadInfo(File file)
      throws IOException, JsonSyntaxException, JsonIOException {
    if (!file.isFile() || !file.exists()) {
      throw new FileNotFoundException(file.getName());
    }
    try (BufferedReader reader = Files.newBufferedReader(file.toPath(), Charsets.UTF_8)) {
      SchematicBookInfo info = GSON.fromJson(reader, SchematicBookInfo.class);
      if (!file.getName().equals(info.getKey() + ".info")) {
        throw new IllegalStateException(
            "SchematicItemInfo has illformated name. Filename and Name must correlate!");
      }
      return info;
    }
  }

  public boolean infoExists(String key) {
    key = key.toLowerCase();
    if (this.schematics.getIfPresent(key) != null) {
      return true;
    }
    File file = getInfoFileOf(key);
    if (file.isFile() && file.exists()) {
      return true;
    }
    return false;
  }

  public boolean schematicExists(String key) {
    File file = getSchematicFileOf(key);
    if (file.isFile() && file.exists()) {
      return true;
    }
    return false;
  }

  public void saveInfo(SchematicBookInfo info) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(getInfoFileOf(info).toPath(),
        Charsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)) {
      GSON.toJson(info, SchematicBookInfo.class, writer);
    }
  }

  public SchematicBookInfo getInfo(String key) throws ExecutionException {
    return schematics.get(key.toLowerCase());
  }

  public SchematicBookInfo getInfo(SchematicEntity entity) throws ExecutionException {
    return getInfo(entity.getKey());
  }

  public Optional<SchematicBookInfo> getInfo(ItemStack item) {
    Optional<String> key = SchematicBook.getSchematicKey(item);
    if (key.isPresent()) {
      try {
        return Optional.of(getInfo(key.get()));
      } catch (ExecutionException e) {
        if (!e.getCause().getClass().equals(FileNotFoundException.class)) {
          throw new RuntimeException(e);
        }
      }
    }
    return Optional.empty();
  }

  public Map<String, SchematicBookInfo> getInfos() {
    return ImmutableMap.copyOf(this.schematics.asMap());
  }

}
