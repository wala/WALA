package testArrayReturn;

public class returnArray {
  public static int[] RetArray(){
    return GetArray();
  }
  private static int[] GetArray(){
      int[] toRet = {1,2,3,4};
      return toRet;
  }

}
