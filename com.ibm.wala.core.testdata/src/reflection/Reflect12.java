package reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Test of Method.invoke
 */
public class Reflect12 {
  public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
    Class c = Class.forName("reflection.Helper");
    Method[] m = c.getMethods();
    m[0].invoke(new Helper(), new Object[3]);
  }
}
