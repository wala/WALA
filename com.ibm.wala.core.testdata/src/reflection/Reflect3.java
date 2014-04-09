package reflection;

import java.util.Hashtable;

public class Reflect3 {

  public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    Class c = Class.forName("java.util.Properties");
    Hashtable h = (Hashtable) c.newInstance();
    System.out.println(h.toString());
  }
  
  @SuppressWarnings("unused")
  private static class Hash extends Hashtable {
    
  }
}
