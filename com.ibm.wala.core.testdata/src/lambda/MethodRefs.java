package lambda;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class MethodRefs {

  static interface I {
    void target();
  }

  static class C1 implements I {

    @Override
    public void target() {}
  }

  static class C2 implements I {

    @Override
    public void target() {}
  }

  static class C3 implements I {

    @Override
    public void target() {}
  }

  static class C4 implements I {

    @Override
    public void target() {}
  }

  static class C5 implements I {

    @Override
    public void target() {}
  }

  static class Dummy {

    Dummy(I i) {
      i.target();
    }
  }

  static void fun1(I i) {
    i.target();
  }

  static void fun1Ref() {
    Consumer<I> c = MethodRefs::fun1;
    I x = new C1();
    c.accept(x);
  }

  void fun2(I i) {
    i.target();
  }

  public static void main(String[] args) {
    fun1Ref();
    MethodRefs r = new MethodRefs();
    Consumer<I> c = r::fun2;
    c.accept(new C2());
    BiConsumer<MethodRefs, I> bc = MethodRefs::fun2;
    bc.accept(r, new C3());
    Function<I, Dummy> f = Dummy::new;
    f.apply(new C4());
  }
}
