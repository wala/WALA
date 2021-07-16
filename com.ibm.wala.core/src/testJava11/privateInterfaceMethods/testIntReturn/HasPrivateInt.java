package testIntReturn;

public class HasPrivateInt {
  public static int C(){//c = speed of light
    return GetC();
  }
  private static int GetC(){
    return 297592;
  }

}
