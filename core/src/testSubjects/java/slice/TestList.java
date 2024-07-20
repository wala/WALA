package slice;

import java.util.ArrayList;
import java.util.List;

public class TestList {

  static void doNothing(Object o) {}

  public static void main(String[] args) {
    List<String> list = new ArrayList<>();

    list.add("hi");
    list.add("bye");

    doNothing(list.get(0));
  }
}
