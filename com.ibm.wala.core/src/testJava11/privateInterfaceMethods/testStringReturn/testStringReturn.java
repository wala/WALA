package testStringReturn;

public class testStringReturn {
  public static void main(String[] args){
    HasPrivateInterface testee = new HasPrivateInterface();
    String test = testee.RetString();
    System.out.println(test);
    return;
  }
}
