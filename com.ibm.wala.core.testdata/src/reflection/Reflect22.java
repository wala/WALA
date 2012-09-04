package reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * Test of Constructor.newInstance
 */
public class Reflect22 {
  public static void main(String[] args) throws ClassNotFoundException, SecurityException,
      NoSuchMethodException, IllegalAccessException, InstantiationException,
      IllegalArgumentException, InvocationTargetException {
    Class helperClass = Class.forName("reflection.Helper");
    Constructor[] constrs = helperClass.getDeclaredConstructors();
    for (Constructor constr : constrs) {
      if (constr.getParameterTypes().length == 1) {
        Class paramType = constr.getParameterTypes()[0];
        if (paramType.getName().equals("java.lang.Integer")) {
          Integer i = new Integer(1);
          Object[] initArgs = new Object[]{i};
          constr.newInstance(initArgs);
          break;
        }
      }
    }
  }
}
