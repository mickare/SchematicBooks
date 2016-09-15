package de.mickare.schematicbooks.util.watcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;

import de.mickare.schematicbooks.util.UnsafeCloseable;

public abstract class HaltablePathWatcher extends PathWatcher {

  private final Set<Path> halted = Collections.synchronizedSet(Sets.newHashSet());

  public HaltablePathWatcher(Path dir, boolean recursive) throws IOException {
    super(dir, recursive);
  }

  public PathWatcher setDebug(boolean debug) {
    super.setDebug(debug);
    return this;
  }

  public UnsafeCloseable halt(Path path) {
    final Path apath = path.toAbsolutePath();
    halted.add(apath);
    return () -> removeHalted(apath);
  }

  private void removeHalted(Path absolutePath) {
    halted.remove(absolutePath);
  }

  public void resume(Path path) {
    removeHalted(path.toAbsolutePath());
  }

  public boolean isHalted(Path path) {
    return halted.contains(path.toAbsolutePath());
  }

  @Override
  protected final void onEntryCreate(Path path) {
    if (!isHalted(path)) {
      onEntryCreate0(path);
    }
  }

  protected abstract void onEntryCreate0(Path path);

  @Override
  protected final void onEntryDelete(Path path) {
    if (!isHalted(path)) {
      onEntryDelete0(path);
    }
  }

  protected abstract void onEntryDelete0(Path path);

  @Override
  protected final void onEntryModify(Path path) {
    if (!isHalted(path)) {
      onEntryModify0(path);
    }
  }

  protected abstract void onEntryModify0(Path path);


}
