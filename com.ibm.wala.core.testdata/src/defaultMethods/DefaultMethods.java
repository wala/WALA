package defaultMethods;

public class DefaultMethods {

  private static class Test1 implements Interface1 {}

  private static class Test2 implements Interface2 {}

  private static class Test3 implements Interface1, Interface2 {
    @Override
    public int silly() {
      return 3;
    }
  }

  public static void main(String[] args) {
    int v1 = (new Test1().silly());
    int v2 = (new Test2().silly());
    int v3 = (new Test3().silly());
  }
}
