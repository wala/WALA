package slice;

import java.util.ArrayList;
import java.util.List;

public class TestListIterator {

  static void doNothing(Object o) {}

  public static void main(String[] args) {
    List<Integer> list = new ArrayList<>();
    list.add(1);
    for (Integer i : list) {
      doNothing(i);
    }
  }
}
