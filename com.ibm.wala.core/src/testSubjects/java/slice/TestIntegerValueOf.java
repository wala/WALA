package slice;

public class TestIntegerValueOf {

  static int getInt() {
    return 0;
  }

  static void doNothing(Object o) {}

  public static void main(String[] args) {
    Integer i = Integer.valueOf(getInt());
    doNothing(i);
  }
}
