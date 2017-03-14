package bug144;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class A {
  void m() {
    Set<Object> set = new HashSet<>();
    Stream<Object> stream = set.parallelStream();
    stream.sorted(); 
    stream.collect(Collectors.toList()); // this call forces the error.
  }
  public static void main(String[] args) {
    new A().m();
  }
}