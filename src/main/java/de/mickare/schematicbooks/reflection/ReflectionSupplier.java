package de.mickare.schematicbooks.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.google.common.base.Preconditions;

import de.mickare.schematicbooks.util.UnsafeSupplier;
import lombok.Getter;

public class ReflectionSupplier<T> implements UnsafeSupplier<T> {

  private final UnsafeSupplier<?> instance;
  private final Method method;
  private final boolean methodStatic;

  private @Getter final Class<T> returnClass;

  public static ReflectionSupplier<?> ofStatic(String methodClass, String methodName,
      String returnClass) throws Exception {
    return ofStatic(methodClass, methodName, Class.forName(returnClass));
  }

  public static <T> ReflectionSupplier<T> ofStatic(String methodClass, String methodName,
      Class<T> returnClass) throws Exception {
    return new ReflectionSupplier<>(null, methodClass, methodName, returnClass);
  }


  public static ReflectionSupplier<?> of(UnsafeSupplier<?> instance, String methodClass,
      String methodName, String returnClass) throws Exception {
    return of(instance, methodClass, methodName, Class.forName(returnClass));
  }

  public static <T> ReflectionSupplier<T> of(UnsafeSupplier<?> instance, String methodClass,
      String methodName, Class<T> returnClass) throws Exception {
    return new ReflectionSupplier<>(instance, methodClass, methodName, returnClass);
  }

  private ReflectionSupplier(UnsafeSupplier<?> instance, String methodClass, String methodName,
      Class<T> returnClass) throws Exception {
    this(instance, Class.forName(methodClass).getDeclaredMethod(methodName), returnClass);
  }

  private ReflectionSupplier(UnsafeSupplier<?> instance, Method method, Class<T> returnClass)
      throws Exception {
    if (instance != null) {
      Preconditions.checkArgument(method.getDeclaringClass().isInstance(instance.get()));
    }
    Preconditions.checkArgument(returnClass.isAssignableFrom(method.getReturnType()));
    final int mod = method.getModifiers();
    Preconditions.checkArgument(Modifier.isPublic(mod), "method not public");
    methodStatic = Modifier.isStatic(mod);
    if (!methodStatic) {
      Preconditions.checkNotNull(instance, "method not static");
      this.instance = instance;
    } else {
      Preconditions.checkArgument(instance == null, "method static, instance not required");
      this.instance = null;
    }
    this.method = method;
    this.returnClass = returnClass;
  }

  @Override
  public T get() throws Exception {
    return getWithInstance(this.instance.get());
  }

  @SuppressWarnings("unchecked")
  public T getWithInstance(Object instance) throws Exception {
    return (T) method.invoke(instance);
  }


}
