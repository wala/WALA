package reflection;

public class ForNameThrownExceptions {

  static class MyClass {
    public String sayHello() {
      return "Hello from MyClass!";
    }
  }

  public static void main(String[] args) {
    try {
      // This call to Class.forName() can be resolved by WALA's reflection handling.  When
      // it is resolved, the resulting synthetic model of Class.forName() cannot throw
      // a ClassNotFoundException.  Hence, no Exception values flow to e, and the call
      // to e.getMessage() in the catch block has no callees
      Class clazz = Class.forName("reflection.ForNameThrownExceptions$MyClass");
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
