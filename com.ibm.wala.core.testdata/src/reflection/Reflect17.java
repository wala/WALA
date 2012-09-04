package reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Test of Method.invoke
 */
public class Reflect17 {
  public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
    Class<?> c = Class.forName("reflection.Helper");
    Method m = c.getDeclaredMethod("t", new Class[] { Integer.class, Integer.class });
    m.invoke(null, new Object[] { null });
  }
}
