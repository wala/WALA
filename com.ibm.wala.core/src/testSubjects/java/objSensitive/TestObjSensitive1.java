package objSensitive;

/**
 * test case for nObjBuilder
 *
 * @author genli
 */
public class TestObjSensitive1 {

  public static void main(String[] args) {
    A a1 = new A();
    A a2 = new A();
    B b1 = new B(); // B/1
    B b2 = new B(); // B/2

    a1.set(b1);
    a2.set(b2);

    B b = a1.getB(); // pts(b) -> {B/1} , n = 1

    doNothing(b);
  }

  static void doNothing(Object o) {}
}
