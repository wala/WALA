package privateInterfaceMethods.testArrayReturn;

import privateInterfaceMethods.testArrayReturn.returnArray;

public class testArrayReturn {
  public void main(String[] args){
    returnArray testee = new returnArray();
    int[] arrayArg = {1,2,3,4};
    testee.RetT(arrayArg);
    return;
  }

}
