package reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Test of Class.getConstructors().
 */
public class Reflect15 {
  public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException {
    Class c = Class.forName("reflection.Helper");
    Constructor[] ctors = c.getConstructors();
    Helper h = null;
    for (Constructor ctor : ctors) {
      if (ctor.getParameterTypes().length == 2) {
        h = (Helper) ctor.newInstance(new Object[] {new Object(), new Object()});
      }
    }
    h.n(new Object(), new Object());
  }
}
