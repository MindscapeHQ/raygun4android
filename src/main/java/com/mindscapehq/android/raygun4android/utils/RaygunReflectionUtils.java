package main.java.com.mindscapehq.android.raygun4android.utils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

public class RaygunReflectionUtils {
  public static Method findMethod(Class<?> clazz, String methodName, Class<?>[] args) throws NoSuchMethodException {
    Method methodMatched = null;

    for (Method m : getAllMethods(clazz)) {

      if (m.getName().equals(methodName)) {
        Class<?>[] paramClasses = m.getParameterTypes();

        if (paramClasses.length == args.length) {
          boolean paramsMatch = true;

          for (int i = 0; i < paramClasses.length; ++i) {
            Class<?> paramType = paramClasses[i];
            paramType = convertPrimitiveClass(paramType);

            if (paramType != args[i]) {
              paramsMatch = false;
              break;
            }
          }

          if (paramsMatch) {
            methodMatched = m;
            break;
          }
        }
      }
    }

    if (methodMatched != null) {
      return methodMatched;
    } else {
      throw new NoSuchMethodException("Cannot find method: " + methodName);
    }
  }

  private static Collection<Method> getAllMethods(Class<?> clazz) {
    HashSet<Method> methods = new HashSet<Method>();

    for (Method m : clazz.getDeclaredMethods()) {
      methods.add(m);
    }

    for (Class<?> s : getAllSuperClasses(clazz)) {
      for (Method m : s.getDeclaredMethods()) {
        methods.add(m);
      }
    }

    return methods;
  }

  private static Collection<Class<?>> getAllSuperClasses(Class<?> clazz) {
    HashSet<Class<?>> classes = new HashSet<Class<?>>();

    if ((clazz != null) && (!clazz.equals(Object.class))) {
      classes.add(clazz);
      classes.addAll(getAllSuperClasses(clazz.getSuperclass()));

      for (Class<?> i : clazz.getInterfaces()) {
        classes.addAll(getAllSuperClasses(i));
      }
    }
    return classes;
  }

  private static Class<?> convertPrimitiveClass(Class<?> primitive) {
    if (primitive.isPrimitive()) {
      if (primitive == Integer.TYPE) {
        return Integer.class;
      }
      if (primitive == Boolean.TYPE) {
        return Boolean.class;
      }
      if (primitive == Float.TYPE) {
        return Float.class;
      }
      if (primitive == Long.TYPE) {
        return Long.class;
      }
      if (primitive == Double.TYPE) {
        return Double.class;
      }
      if (primitive == Short.TYPE) {
        return Short.class;
      }
      if (primitive == Byte.TYPE) {
        return Byte.class;
      }
      if (primitive == Character.TYPE) {
        return Character.class;
      }
    }
    return primitive;
  }
}
