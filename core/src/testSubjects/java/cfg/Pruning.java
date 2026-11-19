package cfg;

public class Pruning {
  public static int effect;

  public static void writeThenThrow(int value, RuntimeException exception) {
    effect = value;
    throw exception;
  }

  public static void writeThenReturn(int value) {
    effect = value;
  }
}
