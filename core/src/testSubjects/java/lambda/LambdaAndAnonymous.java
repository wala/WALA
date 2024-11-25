package lambda;

public class LambdaAndAnonymous {
  interface A {
    void target();
  }

  public static void main(String[] args) {
    A x = new A() {
      @Override
      public void target() {
      }
    };
    x.target();
    A y = () -> {
    };
    y.target();
  }

}
