package reflection;

import java.io.FilePermission;

public class Reflect4 {

  public static void main(String[] args) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
    Class c = Class.forName("java.io.FilePermission");
    FilePermission h = (FilePermission) c.newInstance();
    System.out.println(h.toString());
  }
}
