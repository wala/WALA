package privateInterfaceMethods.testArrayReturn;

public class TestArrayReturn {
  public void main(String[] args) {
    ReturnArray testee = new ReturnArray();
    int[] arrayArg = {1, 2, 3, 4};
    testee.RetT(arrayArg);
  }
}
