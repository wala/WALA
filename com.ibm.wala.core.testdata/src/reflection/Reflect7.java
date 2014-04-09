package reflection;

import java.io.FilePermission;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * 
 * @author pistoia
 * 
 */
public class Reflect7 {
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException,
      IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    Class c = Class.forName("java.io.FilePermission");
    Class[] paramTypes = new Class[] { "".getClass(), "".getClass() };
    Constructor<FilePermission> constr = c.getConstructor(paramTypes);
    Object[] params = new String[] { "log.txt", "read" };
    FilePermission fp = constr.newInstance(params);
    fp.toString();
  }
}
