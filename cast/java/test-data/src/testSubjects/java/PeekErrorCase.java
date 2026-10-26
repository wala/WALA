public class PeekErrorCase {

  public static void main(String[] args) {
    (new PeekErrorCase()).start();
  }

  public int start() {
    System.out.println(""); // Any method invocation here

    //noinspection ConditionalExpressionWithIdenticalBranches,ConstantConditionalExpression
    final int num = true ? 1 : 1; // has to be a ternary?
    Object o = new Object() {
      public int hashCode() {
        return num; // must use num in this function
      }
    };
    return o.hashCode();
  }
}
