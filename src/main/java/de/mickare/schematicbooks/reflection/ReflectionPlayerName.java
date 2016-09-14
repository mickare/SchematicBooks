package de.mickare.schematicbooks.reflection;

import java.util.UUID;
import java.util.function.Function;

import org.bukkit.plugin.Plugin;

import com.google.common.base.Preconditions;

import de.mickare.schematicbooks.util.UnsafeSupplier;

public class ReflectionPlayerName {

  private ReflectionPlayerName() {}

  public static Function<UUID, String> ofStatic(String methodClass, String methodName)
      throws Exception {
    return ReflectionFunction.ofStatic(methodClass, methodName, String.class, UUID.class).unsafe();
  }

  public static Function<UUID, String> ofInstance(String instanceClass, String instanceGetter,
      String methodClass, String methodName) throws Exception {
    return ofInstance(ReflectionSupplier.ofStatic(instanceClass, instanceGetter, instanceClass),
        methodClass, methodName);
  }

  public static Function<UUID, String> ofInstance(UnsafeSupplier<?> instance, String methodClass,
      String methodName) throws Exception {
    return ReflectionFunction.of(instance, methodClass, methodName, String.class, UUID.class)
        .unsafe();
  }

  public static Function<UUID, String> ofPlugin(final Plugin plugin, final String methodName)
      throws Exception {
    Preconditions.checkNotNull(plugin);
    return ofInstance(() -> plugin, plugin.getClass().getName(), methodName);
  }

}
