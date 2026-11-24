package libmodels;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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

  public static void setStandardStreams() {
    System.setIn(new ByteArrayInputStream(new byte[0]));
    System.setOut(new Stdout());
    System.setErr(new Stderr());
  }

  private static class Stdout extends PrintStream {
    Stdout() {
      super(new ByteArrayOutputStream());
    }
  }

  private static class Stderr extends PrintStream {
    Stderr() {
      super(new ByteArrayOutputStream());
    }
  }
}
