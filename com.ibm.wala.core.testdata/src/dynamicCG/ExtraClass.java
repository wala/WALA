package dynamicCG;

public class ExtraClass {
  private final Object x;
  
  public ExtraClass(Object x) {
    this.x = x;
  }

  public ExtraClass() {
    this(null);
  }

  private static String printObject() {
    return " (object)";
  }
  
  private static String getName(Object x) {
    return x.toString() + printObject();
  }
  
  @Override
  public String toString() {
     return getName(x);
   }
}
