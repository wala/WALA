package dynamicCG;

import java.util.HashSet;
import java.util.Set;

public class CallbacksMainClass {

  private static CallbacksMainClass instance;
  
  static {
    callSomethingStatic();
  }
  
  public static void main(String[] args) {
    Set<CallbacksMainClass> junk = new HashSet<CallbacksMainClass>();
    junk.add(instance);
    System.err.println(junk.iterator().next().toString());
  }

  private static void callSomethingStatic() {
    instance = new CallbacksMainClass();
  }

  @Override
  public String toString() {
    return callSomething();
  }

  private String callSomething() {
    return "string";
  }
  
}
