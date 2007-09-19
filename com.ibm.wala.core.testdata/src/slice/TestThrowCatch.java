package slice;

public class TestThrowCatch {

  static class MyException extends Exception {

    int state;

    MyException(int state) {
      this.state = state;
    }

  }

  public static void callee(int x) throws MyException {
    if (x < 3) {
      throw new MyException(x);
    }
  }

  public static void doNothing(int x) {

  }

  public static void main(String args[]) {
    try {
      callee(7);
    } catch (MyException e) {
      int x = e.state;
      doNothing(x);
    }
  }
}