package testIntReturn;

public class testIntReturn {

  public class testStringReturn {
    public void main(String[] args){
      HasPrivateInt testee = new HasPrivateInt();
      int test = testee.c();
      System.out.println(test);
      return;
    }
  }
}
