package lambda;

public class ParamsAndCapture {

  static interface A {

    void target();
  }

  static class C1 implements A {

    @Override
    public void target() {}
  }

  static class C2 implements A {

    @Override
    public void target() {}
  }

  static class C3 implements A {

    @Override
    public void target() {}
  }

  static class C4 implements A {

    @Override
    public void target() {}
  }

  static class C5 implements A {

    @Override
    public void target() {}
  }

  static interface Params {

    void m(A a1, A a2, A a3);
  }

  public static void main(String[] args) {
    A x = new C4(), y = new C5();
    Params p =
        (p1, p2, p3) -> {
          x.target();
          y.target();
          p1.target();
          p2.target();
          p3.target();
        };
    p.m(new C1(), new C2(), new C3());
  }
}
