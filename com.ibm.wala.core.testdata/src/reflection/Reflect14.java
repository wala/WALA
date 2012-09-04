package reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Test of Method.invoke
 */
public class Reflect14 {
  public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
    Class c = Class.forName("reflection.Helper");
    Method[] ms = c.getMethods();
    for (Method m : ms) {
      int mods = m.getModifiers();
      if (Modifier.isStatic(mods) && m.getParameterTypes().length == 2) {
        m.invoke(null, new Object[2]);
      }
    }
  }
}
