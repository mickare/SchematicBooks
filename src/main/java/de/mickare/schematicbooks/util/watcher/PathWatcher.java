package de.mickare.schematicbooks.util.watcher;
/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * - Neither the name of Oracle nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import lombok.Getter;
import lombok.Setter;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

public abstract class PathWatcher implements Closeable, Runnable {

  private final WatchService watcher;
  private final BiMap<WatchKey, Path> keys = HashBiMap.create();
  private @Getter final boolean recursive;
  private @Getter volatile boolean closed = false;
  private @Getter @Setter boolean debug = false;

  @SuppressWarnings("unchecked")
  private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  private void debugFormat(String format, Object... args) {
    if (debug)
      System.out.format(format, args);
  }

  private void debugPrintln(String msg) {
    if (debug)
      System.out.println(msg);
  }

  public boolean isValid() {
    return keys.keySet().stream().filter(WatchKey::isValid).findAny().isPresent();
  }

  /**
   * Register the given directory with the WatchService
   */
  private void register(Path dir) throws IOException {
    if (keys.containsValue(dir)) {
      return;
    }
    WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

    if (debug) {
      Path prev = keys.get(key);
      if (prev == null) {
        System.out.format("register: %s\n", dir);
      } else {
        if (!dir.equals(prev)) {
          System.out.format("update: %s -> %s\n", prev, dir);
        }
      }
    }

    keys.forcePut(key, dir);
  }

  /*
   * private void unregister(Path dir) { WatchKey key = keys.inverse().remove(dir); if (key != null)
   * { key.cancel(); } }
   */


  /**
   * Register the given directory, and all its sub-directories, with the WatchService.
   */
  private void registerAll(final Path start) throws IOException {
    // register directory and sub-directories
    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        register(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /*
   * private void unregisterAll(final Path start) throws IOException { Files.walkFileTree(start, new
   * SimpleFileVisitor<Path>() {
   * 
   * @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws
   * IOException { unregister(dir); return FileVisitResult.CONTINUE; } }); }
   */


  /**
   * Creates a WatchService and registers the given directory
   */
  public PathWatcher(Path dir, boolean recursive) throws IOException {
    this(dir, recursive, false);
  }

  /**
   * Creates a WatchService and registers the given directory
   */
  public PathWatcher(Path dir, boolean recursive, boolean debug) throws IOException {
    this.watcher = FileSystems.getDefault().newWatchService();
    this.recursive = recursive;
    this.debug = debug;

    if (recursive) {
      debugFormat("Scanning %s ...\n", dir);
      registerAll(dir);
      debugPrintln("Done.");
    } else {
      register(dir);
    }

    // enable trace after initial registration
    this.debug = true;
  }

  public void close() throws IOException {
    closed = true;
    this.watcher.close();
  }

  /**
   * Process all events for keys queued to the watcher
   */
  protected void processEvents() {

    if (closed) {
      return;
    }

    WatchKey key = null;

    while (true) {

      key = watcher.poll();
      if (key == null || !key.isValid()) {
        return;
      }

      Path dir = keys.get(key);
      if (dir == null) {
        continue;
      }

      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind<?> kind = event.kind();

        // TBD - provide example of how OVERFLOW event is handled
        if (kind == OVERFLOW) {
          continue;
        }

        // Context for directory entry event is the file name of entry
        WatchEvent<Path> ev = cast(event);
        Path name = ev.context();
        Path child = dir.resolve(name);

        // print out event
        debugFormat("%s: %s\n", event.kind().name(), child);

        try {

          if (kind == ENTRY_CREATE) {
            onEntryCreate(child);
          } else if (kind == ENTRY_CREATE) {
            onEntryCreate(child);
          } else if (kind == ENTRY_CREATE) {
            onEntryCreate(child);
          }

        } catch (Exception e) {
          System.out.print("Exception in " + this.getClass().getName() + "\n" + e.getMessage());
          e.printStackTrace();
        }

        // if directory is created, and watching recursively, then
        // register it and its sub-directories
        if (recursive && (kind == ENTRY_CREATE)) {
          try {
            if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
              registerAll(child);
            }
          } catch (IOException x) {
            // ignore to keep sample readbale
          }
        }
      }

      // reset key and remove from set if directory no longer accessible
      boolean valid = key.reset();
      if (!valid) {
        keys.remove(key);

        // all directories are inaccessible
        if (keys.isEmpty()) {
          break;
        }
      }
    }

  }

  public void run() {
    this.processEvents();
  }

  protected abstract void onEntryCreate(Path path);

  protected abstract void onEntryDelete(Path path);

  protected abstract void onEntryModify(Path path);


}
