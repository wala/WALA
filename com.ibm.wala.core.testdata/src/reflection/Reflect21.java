package reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * Test of Constructor.newInstance
 */
public class Reflect21 {
  public static void main(String[] args) throws ClassNotFoundException, SecurityException,
      NoSuchMethodException, IllegalAccessException, InstantiationException,
      IllegalArgumentException, InvocationTargetException {
    Class<?> helperClass = Class.forName("reflection.Helper");
    Class objectClass = Class.forName("java.lang.Object");
    Class[] paramArrayTypes = new Class[]{objectClass, objectClass};
    Constructor constr = helperClass.getDeclaredConstructor(paramArrayTypes);
    Object[] paramArrayObjects = new Object[]{new Object(), new Object()};
    constr.newInstance(paramArrayObjects);
  }
}
