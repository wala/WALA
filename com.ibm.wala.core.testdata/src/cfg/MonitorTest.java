package cfg;

public class MonitorTest {
  void sync1() {
    Object a = new Object();
    synchronized (this) {
      synchronized (a) {
        dummy();
      }
    }
  }

  void sync2() {
    Object a = new Object();
    synchronized (this) {
      synchronized (a) {
        // Nothing here.
      }
    }
  }

  void dummy() {
  }
}