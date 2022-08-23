public class StringsOnlyGetMessage {

  public static void main(String[] args) {
    try {
      Class clazz = Class.forName("MyClass");
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
