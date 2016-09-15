package de.mickare.schematicbooks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

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
import de.mickare.schematicbooks.util.UnsafeCloseable;
import de.mickare.schematicbooks.util.watcher.HaltablePathWatcher;
import lombok.Getter;

public class InfoManager {

  private @Getter final SchematicBooksPlugin plugin;
  private final InfoWatcher watcher;

  private static Gson GSON = new GsonBuilder().create();
  private LoadingCache<String, SchematicBookInfo> schematics = CacheBuilder.newBuilder()//
      .build(new CacheLoader<String, SchematicBookInfo>() {

        @Override
        public SchematicBookInfo load(String key) throws Exception {
          return loadInfo(getInfoFileOf(key).toFile());
        }
      });

  public InfoManager(SchematicBooksPlugin plugin) throws IOException {
    Preconditions.checkNotNull(plugin);
    this.plugin = plugin;

    getInfoFiles().forEach(path -> {
      File infofile = path.toFile();
      try {
        SchematicBookInfo info = loadInfo(infofile);
        schematics.put(info.getKey().toLowerCase(), info);
      } catch (IOException | JsonSyntaxException | JsonIOException e) {
        logger().log(Level.WARNING, "Could not load schematic info " + infofile.getName(), e);
      }
    });

    watcher = new InfoWatcher(plugin.getSchematicFolder(), false);
    new BukkitRunnable() {
      @Override
      public void run() {
        if (watcher.isClosed() && watcher.isValid()) {
          this.cancel();
          return;
        }
        watcher.run();
      }
    }.runTaskTimerAsynchronously(plugin, 100, 100);


  }

  public void invalidateAll() {
    this.schematics.invalidateAll();
  }

  private Logger logger() {
    return plugin.getLogger();
  }


  public Path getInfoFileOf(String key) {
    return plugin.getSchematicFolder().resolve(key.toLowerCase() + ".info");
  }

  public Path getInfoFileOf(SchematicBookInfo info) {
    return getInfoFileOf(info.getKey());
  }

  public Path getSchematicFileOf(String key) {
    return plugin.getSchematicFolder().resolve(key.toLowerCase() + ".schematic");
  }

  public Path getSchematicFileOf(SchematicBookInfo info) {
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
    File file = getInfoFileOf(key).toFile();
    if (file.isFile() && file.exists()) {
      return true;
    }
    return false;
  }

  public boolean schematicExists(String key) {
    File file = getSchematicFileOf(key).toFile();
    if (file.isFile() && file.exists()) {
      return true;
    }
    return false;
  }

  public void saveInfo(SchematicBookInfo info) throws IOException {
    final Path path = getInfoFileOf(info);


    try (UnsafeCloseable halt = this.watcher.halt(path); //
        BufferedWriter writer =
            Files.newBufferedWriter(path, Charsets.UTF_8, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
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

  public Map<String, SchematicBookInfo> getAllInfos() {
    return ImmutableMap.copyOf(this.schematics.asMap());
  }

  public Stream<Path> getInfoFiles() throws IOException {
    return Files.find(plugin.getSchematicFolder(), 0,
        (path, attr) -> attr.isRegularFile() && path.endsWith(".info"));
  }

  private static String getBookKeyOfPath(Path path) {
    Preconditions.checkArgument(path.endsWith(".info"));
    String filename = path.getFileName().toString();
    return filename.substring(0, filename.lastIndexOf(".info"));
  }

  private class InfoWatcher extends HaltablePathWatcher {

    public InfoWatcher(Path dir, boolean recursive) throws IOException {
      super(dir, recursive);
    }

    private void refreshInfoPath(Path path) {
      if (path.endsWith(".info")) {
        try {
          if (Files.size(path) > 0) {
            InfoManager.this.schematics.refresh(getBookKeyOfPath(path));
          }
        } catch (IOException e) {
        }
      }
    }

    @Override
    protected void onEntryCreate0(Path path) {
      refreshInfoPath(path);
    }

    @Override
    protected void onEntryDelete0(Path path) {
      if (path.endsWith(".info")) {
        InfoManager.this.schematics.invalidate(getBookKeyOfPath(path));
      }
    }

    @Override
    protected void onEntryModify0(Path path) {
      refreshInfoPath(path);
    }

  }

}
