package cfg;

public class IteratorSubject {
  public static void empty() {}

  public static int branch(Object value) {
    int result;
    if (value instanceof String) {
      result = value.hashCode();
    } else {
      result = 7;
    }
    return result;
  }

  public static int catchException(int[] values) {
    try {
      return values[0];
    } catch (ArrayIndexOutOfBoundsException e) {
      return -1;
    }
  }
}
