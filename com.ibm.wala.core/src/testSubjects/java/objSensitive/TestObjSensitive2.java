package objSensitive;
/**
 * test case for nObjBuilder
 *
 * @author genli
 */
public class TestObjSensitive2 {

  public static void main(String[] args) {
    A a1 = new A();
    Object o1 = new Object(); //  Object/1
    Object result1 = a1.foo(o1);

    A a2 = new A();
    Object o2 = new Object(); //   Object/2
    Object result2 = a2.foo(o2); //   pts(result2) -> {Object/2} , n = 3

    doNothing(result2);
  }

  static void doNothing(Object o) {}
}
