package de.mickare.schematicbooks.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import com.google.common.base.Preconditions;

public class ReflectUtils {

  private ReflectUtils() {}


  public static Method getFirstStaticMethod(Class<?> c, Type returnType, Type... parameters) {
    Preconditions.checkNotNull(c);
    Preconditions.checkNotNull(returnType);
    for (Method m : c.getMethods()) {
      if (Modifier.isStatic(c.getModifiers()) && Modifier.isPublic(m.getModifiers())) {
        if (m.getReturnType().equals(returnType)) {
          final Class<?>[] mParas = m.getParameterTypes();
          if (mParas.length == parameters.length) {
            for (int i = 0; i < mParas.length; ++i) {
              if (!mParas[i].equals(parameters)) {
                continue;
              }
            }
            // Found
            return m;
          }
        }
      }
    }

    return null;
  }


  public static Method getFirstMethod(Class<?> c, Type returnType, Type... parameters) {
    Preconditions.checkNotNull(c);
    Preconditions.checkNotNull(returnType);
    for (Method m : c.getMethods()) {
      if (!Modifier.isStatic(c.getModifiers()) && Modifier.isPublic(m.getModifiers())) {
        if (m.getReturnType().equals(returnType)) {
          final Class<?>[] mParas = m.getParameterTypes();
          if (mParas.length == parameters.length) {
            for (int i = 0; i < mParas.length; ++i) {
              if (!mParas[i].equals(parameters)) {
                continue;
              }
            }
            // Found
            return m;
          }
        }
      }
    }

    return null;
  }

}
