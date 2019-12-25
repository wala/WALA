package javaonepointseven;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Linghui Luo
 * @see
 *     <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/language/type-inference-generic-instance-creation.html">Java 7 docs</a>
 */
public class TypeInferenceforGenericInstanceCreation {
  class MyClass<X> {
    X a;
    <T> MyClass(T t) {
      System.out.println("constructor");
    }
  }

  public void test() {
    List<String> list = new ArrayList<>();
    list.add("A");
    list.addAll(new ArrayList<>());

    List<? extends String> list2 = new ArrayList<>();
    list.addAll(list2);

    MyClass<Integer> myObject = new MyClass<>("");
  }
  
  public static void main(String[] args) {
	  new TypeInferenceforGenericInstanceCreation().test();
  }
}
