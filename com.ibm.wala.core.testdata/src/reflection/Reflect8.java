package reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Test of Class.getConstructors().
 */
public class Reflect8 {
  public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException {
    Class c = Class.forName("java.lang.Integer");
    Constructor[] ctors = c.getConstructors();
    Integer i = (Integer) ctors[0].newInstance(new Integer(1));
    i.toString();
  }
}
