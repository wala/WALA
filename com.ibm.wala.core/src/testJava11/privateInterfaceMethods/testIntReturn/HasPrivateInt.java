package testIntReturn;

public class HasPrivateInt {
  public static int c(){
    return getC();
  }
  private static int getC(){
    return 297592;
  }

}
