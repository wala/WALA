public class StaticInit {

  static class X {
    int x;
    int y;

    int sum() {
      return x+y;
    }

    int diff() {
      return x+-y;
    }
  }

  private static X x = new X();
  private static X y;

  static {
    y = new X();
  }

  private static int sum() {
    return x.sum() * y.diff();
  }

  public static void main(String[] args) {
    StaticInit SI = new StaticInit();
    SI.sum();
  }
}
