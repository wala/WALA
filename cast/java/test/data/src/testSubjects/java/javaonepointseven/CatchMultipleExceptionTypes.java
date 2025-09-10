package javaonepointseven;

/**
 * @author Linghui Luo
 * @see <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/language/catch-multiple.html">Java 7 docs</a>
 */
public class CatchMultipleExceptionTypes {

public static void main(String[] args) {
	new CatchMultipleExceptionTypes().test(-1,  new int[] {0, 1});
}

  public void test(int i, int[] arr) {
    try {
      int num = 100;
      int a = 100 / i;
      int b = arr[i];
      System.out.println(a + b);
    } catch (ArithmeticException | IndexOutOfBoundsException ex) {
      throw ex;
    }
  }
}
