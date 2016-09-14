package de.mickare.schematicbooks.util;

import java.util.function.Supplier;

@FunctionalInterface
public interface UnsafeSupplier<T> {

  T get() throws Exception;

  default Supplier<T> unsafe() {
    return () -> {
      try {
        return this.get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

}
