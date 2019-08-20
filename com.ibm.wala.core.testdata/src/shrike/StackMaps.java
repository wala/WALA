package shrike;

public class StackMaps {
  private int field1;
  private int field2;
  private int min;

  public StackMaps(int field1, int field2) {
    setMin(field1 > field2 ? field2 : field1);
    this.field1 = field1;
    this.field2 = field2;
  }

  private void setMin(int x) {
    this.min = x;
  }

  public static void main(String[] args) {
    String result = "success";
    try {
      if ("max".equals(args[0])) {
        System.err.println(new StackMaps(3, 2).max());
      } else {
        System.err.println(new StackMaps("min" == args[0] ? 7 : 5, 2).min());
      }
    } catch (RuntimeException e) {
      result = "bad";
      throw e;
    } finally {
      System.err.println(result);
    }
  }

  public int max() {
    return field1 > field2 ? field1 : field2;
  }

  public int min() {
    return min;
  }
}
