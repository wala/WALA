package dynamicCG;

public class MainClass {
  private final Object x;
    
  private MainClass(Object x) {
    this.x = x;
  }

  private final String printNull() {
    return "*null*";
  }
  
  private String callSomething(Object x) {
    return "mc:" + (x==null? printNull(): x.toString());
  }
  
  private String toStringImpl() {
    try {
      return "mc:" + x.toString();
    } catch (NullPointerException e) {
      return callSomething(x);
    }
  }
  
  @Override
  public String toString() {
    return toStringImpl();
  }
  
  public static void main(String[] args) {
    MainClass mc = new MainClass(new ExtraClass("ExtraClass"));
    System.err.println(mc.toString());
    mc = new MainClass(null);
    System.err.println(mc.toString());
    mc = new MainClass(new ExtraClass());
    System.err.println(mc.toString());
  }
}
