package de.mickare.schematicbooks.util;

import java.io.Closeable;

public interface UnsafeCloseable extends Closeable {
  void close();
}
