package objSensitive;

/** @author genli */
public class B {
  Object bar(Object v) {
    C c = new C();
    return c.identity(v);
  }
}
