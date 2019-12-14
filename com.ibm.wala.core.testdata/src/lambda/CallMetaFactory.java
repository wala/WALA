package lambda;

import java.lang.invoke.LambdaMetafactory;

public class CallMetaFactory {

  public static void main(String[] args) {
    // shouldn't crash on this
    LambdaMetafactory m = new LambdaMetafactory();
  }
}
