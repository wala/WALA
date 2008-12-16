public class Monitor2 {
  int i = 0;

  public Monitor2() { }

  public void incr() { synchronized(this) { i++; } }

  private static boolean test(Object o) {
    return true;
  }

  public static void main(String[] a) {
    new Monitor2().incr();
  }
}
