package lambda;

import java.lang.invoke.LambdaMetafactory;

public class CallMetaFactory {

  public static void main(String[] args) throws IllegalAccessException, InstantiationException {
    // shouldn't crash on this
    LambdaMetafactory m = LambdaMetafactory.class.newInstance();
  }
}
