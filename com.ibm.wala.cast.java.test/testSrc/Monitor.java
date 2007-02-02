public class Monitor {
  int i = 0;

  public Monitor() { }

  public void incr() { synchronized(this) { i++; } }

  public static void main(String[] a) {
    new Monitor().incr();
  }
}
