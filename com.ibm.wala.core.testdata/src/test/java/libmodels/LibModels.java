package libmodels;

public class LibModels {

  public static void reachable1() {}

  public static void reachable2() {}

  static class Handler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
      reachable2();
    }
  }

  public static void main(String[] argv) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                reachable1();
              }
            });
    Thread.setDefaultUncaughtExceptionHandler(new Handler());
  }
}
