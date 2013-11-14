package dynamicCG;

public class MainClass {
  private final Object x;
  
  private MainClass(Object x) {
    this.x = x;
  }

  private String toStringImpl() {
    return "mc:" + x.toString();
  }
  
  @Override
  public String toString() {
    return toStringImpl();
  }
  
  public static void main(String[] args) {
    MainClass mc = new MainClass(new ExtraClass());
    System.err.println(mc.toString());
  }
}
