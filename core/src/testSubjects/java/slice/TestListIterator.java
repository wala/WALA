package slice;

import java.util.Iterator;

public class TestListIterator {

  interface List<E> extends Iterable<E> {
    void add(E e);
  }

  static class ArrayList<E> implements List<E> {
    private E[] elements;
    private int size = 0;

    @SuppressWarnings("unchecked")
    public ArrayList() {
      elements = (E[]) new Object[10];
    }

    @Override
    public void add(E e) {
      if (size == elements.length) {
        @SuppressWarnings("unchecked")
        E[] newElements = (E[]) new Object[elements.length * 2];
        System.arraycopy(elements, 0, newElements, 0, elements.length);
        elements = newElements;
      }
      elements[size++] = e;
    }

    @Override
    public Iterator<E> iterator() {
      return new Iterator<E>() {
        private int index = 0;

        @Override
        public boolean hasNext() {
          return index < size;
        }

        @Override
        public E next() {
          return elements[index++];
        }
      };
    }
  }

  static void doNothing(Object o) {}

  public static void main(String[] args) {
    List<Integer> list = new ArrayList<>();
    list.add(1);
    for (Integer i : list) {
      doNothing(i);
    }
  }
}
