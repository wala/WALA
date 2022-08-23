package reflection;

public class StringsOnlyGetMessage {

  static class MyClass {
    public String sayHello() {
      return "Hello from MyClass!";
    }
  }

  public static void main(String[] args) {
    try {
      Class clazz = Class.forName("reflection.StringsOnlyGetMessage$MyClass");
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
