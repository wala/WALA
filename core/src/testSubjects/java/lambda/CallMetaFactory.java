package lambda;

import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;

public class CallMetaFactory {

  public static void main(String[] args)
      throws IllegalAccessException, InstantiationException, LambdaConversionException {
    // shouldn't crash on this
    LambdaMetafactory m = LambdaMetafactory.class.newInstance();

    // shouldn't crash on this either
    LambdaMetafactory.altMetafactory(null, null, null);
  }
}
