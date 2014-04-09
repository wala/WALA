package reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Test of Constructor.newInstance
 */
public class Reflect19 {
  public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
    Class<?> c = Class.forName("reflection.Helper");
    Constructor m = c.getConstructor(new Class[] { Integer.class });
    Integer i = Integer.valueOf(0);
    m.newInstance(new Object[] { i });
  }
}
