package multiDim;

public class TestMultiDim {

  static void doNothing(Object o) {}
 
  public static void main(String[] args) {
    Object[][] multi = new Object[10][];
    multi[0] = new Object[10];
    testMulti(multi);
  }
 
  static void testMulti(Object[][] multi) {
    Object[] t = multi[0];
    doNothing(t);
  }

  static void testNewMultiArray() {
    String[][][] x = new String[3][4][];
    doNothing(x);
  }
}