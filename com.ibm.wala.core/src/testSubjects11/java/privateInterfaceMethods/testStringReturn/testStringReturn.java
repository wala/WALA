package privateInterfaceMethods.testStringReturn;

public class testStringReturn {
  public static void main(String[] args) {
    HasPrivateInterface testee = new HasPrivateInterface();
    testee.RetT("Hello World!");
    return;
  }
}
