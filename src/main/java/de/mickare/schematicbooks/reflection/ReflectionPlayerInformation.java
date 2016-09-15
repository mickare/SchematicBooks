package de.mickare.schematicbooks.reflection;

import java.util.UUID;
import java.util.function.Function;

import org.bukkit.plugin.Plugin;

import com.google.common.base.Preconditions;

import de.mickare.schematicbooks.util.UnsafeSupplier;

public class ReflectionPlayerInformation {

  private ReflectionPlayerInformation() {}

  public static Function<UUID, String> nameByStatic(String methodClass, String methodName)
      throws Exception {
    return ReflectionFunction.ofStatic(methodClass, methodName, String.class, UUID.class).unsafe();
  }

  public static Function<UUID, String> nameByInstance(String instanceClass, String instanceGetter,
      String methodClass, String methodName) throws Exception {
    return nameByInstance(ReflectionSupplier.ofStatic(instanceClass, instanceGetter, instanceClass),
        methodClass, methodName);
  }

  public static Function<UUID, String> nameByInstance(UnsafeSupplier<?> instance,
      String methodClass, String methodName) throws Exception {
    return ReflectionFunction.of(instance, methodClass, methodName, String.class, UUID.class)
        .unsafe();
  }

  public static Function<UUID, String> nameByPlugin(final Plugin plugin, final String methodName)
      throws Exception {
    Preconditions.checkNotNull(plugin);
    return nameByInstance(() -> plugin, plugin.getClass().getName(), methodName);
  }



  public static Function<String, UUID> uuidByStatic(String methodClass, String methodName)
      throws Exception {
    return ReflectionFunction.ofStatic(methodClass, methodName, UUID.class, String.class).unsafe();
  }

  public static Function<String, UUID> uuidByInstance(String instanceClass, String instanceGetter,
      String methodClass, String methodName) throws Exception {
    return uuidByInstance(ReflectionSupplier.ofStatic(instanceClass, instanceGetter, instanceClass),
        methodClass, methodName);
  }

  public static Function<String, UUID> uuidByInstance(UnsafeSupplier<?> instance,
      String methodClass, String methodName) throws Exception {
    return ReflectionFunction.of(instance, methodClass, methodName, UUID.class, String.class)
        .unsafe();
  }

  public static Function<String, UUID> uuidByPlugin(final Plugin plugin, final String methodName)
      throws Exception {
    Preconditions.checkNotNull(plugin);
    return uuidByInstance(() -> plugin, plugin.getClass().getName(), methodName);
  }

}
