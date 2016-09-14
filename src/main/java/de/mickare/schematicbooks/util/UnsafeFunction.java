package de.mickare.schematicbooks.util;

import java.util.function.Function;

@FunctionalInterface
public interface UnsafeFunction<T, R> {

  R apply(T t) throws Exception;

  default Function<T, R> unsafe() {
    return (t) -> {
      try {
        return this.apply(t);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

}
