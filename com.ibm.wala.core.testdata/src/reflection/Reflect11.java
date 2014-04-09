package reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Test of Method.invoke
 */
public class Reflect11 {
  public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
    Class c = Class.forName("java.lang.Integer");
    Method[] m = c.getMethods();
    m[0].invoke(new Integer(2), (Object[]) args);
  }
}
