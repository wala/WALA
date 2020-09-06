package objSensitive;

/** @author genli */
public class A {

  private B b;

  public void set(B b) {
    doSet(b);
  }

  public void doSet(B b) {
    this.b = b;
  }

  public B getB() {
    return this.b;
  }

  public Object foo(Object v) {
    B b = new B();
    return b.bar(v);
  }
}
