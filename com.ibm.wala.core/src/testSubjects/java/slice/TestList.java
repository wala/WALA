package slice;

import java.util.ArrayList;
import java.util.List;

public class TestList {

  static void doNothing(Object o) {}

  public static void main(String[] args) {
    List<Integer> list = new ArrayList<>();

    list.add(2);
    list.add(3);

    doNothing(list.get(0));
  }
}
