package reflection;

public class Helper {
  @SuppressWarnings("unused")
  private final Object a, b;
  
  public Helper() {
    this.a = new Object();
    this.b = new Object();
    System.out.println("Helper constructor with no parameter invoked");
  }
  
  public Helper(Object a) {
    this.a = a;
    this.b = new Object();
    System.out.println("Helper constructor with one parameter invoked");
  }
  
  public Helper(Object a, Object b) {
    this.a = a;
    this.b = b;
    System.out.println("Helper constructor with two parameters invoked");
  }
  
  public void m(Object a, Object b, Object c) {
    System.out.println("m method invoked");
  }
  public void n(Object a, Object b) {
    System.out.println("n method invoked");
  }
  public static void s(Object a, Object b) {
    System.out.println("s method invoked");
  }
}
