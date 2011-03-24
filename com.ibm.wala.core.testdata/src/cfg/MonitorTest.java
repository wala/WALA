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

  void sync3() {
    Object a = new Object();
    Object b = new Object();
    Object c = new Object();
    synchronized (a) {
      synchronized (b) {
        synchronized (c) {
          dummy();
        }
      }
    }
  }

  void dummy() {
  }
}