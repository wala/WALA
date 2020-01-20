package dynamicCG;

import java.util.HashSet;
import java.util.Set;

public class CallbacksMainClass {

  private static CallbacksMainClass instance;

  public static class Junk {

    static {
      callSomethingStatic();
    }
  }

  static {
    new Junk();
  }

  public static void main(String[] args) {
    Set<CallbacksMainClass> junk = new HashSet<>();
    junk.add(instance);
    System.err.println(junk.iterator().next().toString());
  }

  public static void callSomethingStatic() {
    instance = new CallbacksMainClass();
  }

  @Override
  public String toString() {
    return callSomething();
  }

  public String callSomething() {
    return "string";
  }
}
