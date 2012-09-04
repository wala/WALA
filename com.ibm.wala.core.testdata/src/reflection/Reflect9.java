package reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Test of Method.invoke
 */
public class Reflect9 {
  public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
    Class<?> c = Class.forName("java.lang.Integer");
    Method m = c.getDeclaredMethod("toString", new Class[] {});
    m.invoke(new Integer(2), new Object[] {});
  }
}
