class R implements Runnable {
  public int i;
	
  R(int i) { this.i = i; }

  public void run() {
    return;
  }

}

public class Thread1 {
  
  private void test() {
    R r = new R(2);
    Thread t = new Thread(r);
    t.start();
  }

  public static void main(String[] a) {
    (new Thread1()).test();
  }

}


