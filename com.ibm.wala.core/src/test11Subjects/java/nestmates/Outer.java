package nestmates;

public class Outer {
  private int foo = 10;

  public class Inner {
    int triple() {
      return 3 * foo;
    }
  }
}
