package dynamicCG;

public class ExtraClass {

  public ExtraClass() {

  }

  private static String getName() {
    return "ExtraClass";
  }
  
  @Override
  public String toString() {
     return getName();
   }
}
