package testIntReturn;

public class testIntReturn {

  public class testStringReturn {
    public void main(String[] args){
      HasPrivateInt testee = new HasPrivateInt();
      int test = testee.C();
      System.out.println(test);
      return;
    }
  }
}
