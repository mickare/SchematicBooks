package de.mickare.schematicbooks.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.google.common.base.Preconditions;

import de.mickare.schematicbooks.util.UnsafeFunction;
import de.mickare.schematicbooks.util.UnsafeSupplier;
import lombok.Getter;

public class ReflectionFunction<T, R> implements UnsafeFunction<T, R> {

  private final UnsafeSupplier<?> instance;
  private final Method method;
  private final boolean methodStatic;

  private @Getter final Class<T> parameterClass;
  private @Getter final Class<R> returnClass;

  public static ReflectionFunction<?, ?> ofStatic(String methodClass, String methodName,
      String returnClass, String parameterClass) throws Exception {
    return ofStatic(methodClass, methodName, Class.forName(returnClass),
        Class.forName(parameterClass));
  }

  public static <T, R> ReflectionFunction<T, R> ofStatic(String methodClass, String methodName,
      Class<R> returnClass, Class<T> parameterClass) throws Exception {
    return new ReflectionFunction<>(null, methodClass, methodName, returnClass, parameterClass);
  }


  public static ReflectionFunction<?, ?> of(UnsafeSupplier<?> instance, String methodClass,
      String methodName, String returnClass, String parameterClass) throws Exception {
    return of(instance, methodClass, methodName, Class.forName(returnClass),
        Class.forName(parameterClass));
  }

  public static <T, R> ReflectionFunction<T, R> of(UnsafeSupplier<?> instance, String methodClass,
      String methodName, Class<R> returnClass, Class<T> parameterClass) throws Exception {
    return new ReflectionFunction<>(instance, methodClass, methodName, returnClass, parameterClass);
  }

  private ReflectionFunction(UnsafeSupplier<?> instance, String methodClass, String methodName,
      Class<R> returnClass, Class<T> parameterClass) throws Exception {
    this(instance, Class.forName(methodClass).getDeclaredMethod(methodName, parameterClass),
        returnClass, parameterClass);
  }

  private ReflectionFunction(UnsafeSupplier<?> instance, Method method, Class<R> returnClass,
      Class<T> parameterClass) throws Exception {
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
      // Preconditions.checkArgument(instance == null, "method static, instance not required");
      this.instance = null;
    }
    this.method = method;
    this.parameterClass = parameterClass;
    this.returnClass = returnClass;
  }

  @Override
  public R apply(T t) throws Exception {
    return applyWithInstance(this.instance != null ? this.instance.get() : null, t);
  }

  @SuppressWarnings("unchecked")
  public R applyUnsafe(Object obj) throws Exception {
    Preconditions.checkArgument(this.parameterClass.isInstance(obj));
    return apply((T) obj);
  }

  @SuppressWarnings("unchecked")
  public R applyWithInstance(Object instance, T t) throws Exception {
    return (R) method.invoke(instance, t);
  }

}
