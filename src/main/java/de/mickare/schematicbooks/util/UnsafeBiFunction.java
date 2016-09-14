package de.mickare.schematicbooks.util;

import java.util.function.BiFunction;

@FunctionalInterface
public interface UnsafeBiFunction<T, U, R> {

  R apply(T t, U u) throws Exception;

  default BiFunction<T, U, R> unsafe() {
    return (t, u) -> {
      try {
        return this.apply(t, u);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

}
