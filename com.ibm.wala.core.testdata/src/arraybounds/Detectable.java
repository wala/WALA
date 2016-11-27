package arraybounds;

/**
 * 
 * All array accesses in the following class are unnecessary and they will be
 * detected correctly by the array bounds analysis.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class Detectable {
  private int[] memberArr = new int[5];

  /**
   * Note: This is correct, even if memberArr is not final!
   * 
   * @param i
   * @return memberArr[i]
   */
  public int memberLocalGet(int i) {
    int[] arr = memberArr;
    if (i >= 0 && i < arr.length) {
      return arr[i];
    } else {
      throw new IllegalArgumentException();
    }
  }

  public int[] constantCreation() {
    return new int[] { 3, 4, 5 };
  }

  public int get(int i, int[] arr) {
    if (i >= 0 && i < arr.length) {
      return arr[i];
    } else {
      throw new IllegalArgumentException();
    }
  }

  public void loop(int[] arr) {
    for (int i = 0; i < arr.length; i++) {
      arr[i] = 0;
    }
  }

  public boolean equals(int[] a, int[] b) {
    int lenA = a.length;
    int lenB = b.length;
    boolean result;

    if (lenA == lenB) {
      result = true;
      for (int i = 0; i < lenA; i++) {
        if (a[i] != b[i]) {
          result = false;
        }
      }
    } else {
      result = false;
    }

    return result;
  }

  public void copy(int[] src, int[] dst) {
    int lenSrc = src.length;
    int lenDst = dst.length;

    if (lenSrc < lenDst) {
      for (int i = 0; i < lenSrc; i++) {
        dst[i] = src[i];
      }
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * swaps elements of a and b for all i: 0 &lt;= i &lt; min(a.length, b.length)
   * 
   * @param a
   * @param b
   */
  public void swapWithMin(int[] a, int[] b) {
    final int l1 = a.length;
    final int l2 = b.length;
    int length;
    if (l1 < l2) {
      length = l1;
    } else {
      length = l2;
    }

    for (int i = 0; i < length; i++) {
      int tmp = a[i];
      a[i] = b[i];
      b[i] = tmp;
    }
  }

  /**
   * Invert the order of all elements of arr with index i: fromIndex &lt;= i <
   * toIndex.
   * 
   * @param arr
   * @param fromIndex
   * @param toIndex
   */
  public void partialInvert(int[] arr, int fromIndex, int toIndex) {
    if (fromIndex >= 0 && toIndex <= arr.length && fromIndex < toIndex) {
      for (int next = fromIndex; next < toIndex; ++next) {
        int tmp = arr[toIndex - 1];
        int i;
        for (i = toIndex - 1; i > next; --i) {
          arr[i] = arr[i - 1];
        }
        arr[i] = tmp; // i == next
      }
    }
  }

  /**
   * The constant 3 is stored in a variable. The pi construction for the
   * variable allows to detect, that the array access is in bound.
   * 
   * Compare to {@link NotDetectable#dueToConstantPropagation(int[])}
   * 
   * @param arr
   * @return arr[3]
   */
  public int nonFinalConstant(int[] arr) {
    int i = 3;
    if (i < arr.length) {
      return arr[i];
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Workaround for {@link NotDetectable#constants(int[])}
   * 
   * Note: It is important, that the variable five, is compared directly to the
   * length, and that further computations are performed with this variable.
   * 
   * @param arr
   * @return arr[3]
   */
  public int constantsWorkaround(int[] arr) {
    int five = 5;
    if (arr.length > five) {
      return arr[five - 2];
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Actually aliasing is only working, because it is removed during
   * construction by wala.
   * 
   * @param i
   * @param arr
   * @return arr[i]
   */
  public int aliasing(int i, int[] arr) {
    int[] a = arr;
    int[] b = arr;

    if (0 <= i && i < a.length) {
      return b[i];
    } else {
      throw new IllegalArgumentException();
    }
  }

  public String afterLoop(int[] arr) {
    int len = arr.length - 1;
    StringBuffer buffer = new StringBuffer();
    int zero = 0;
    if (zero < arr.length) {
      int i = zero;
      while (i < len) {
        buffer.append(arr[i]);
        buffer.append(", ");
        i++;
      }

      buffer.append(arr[i]);
    }
    return buffer.toString();
  }

  public static void quickSort(int[] arr, int left, int right) {
    if (0 <= left && right <= arr.length && left < right - 1) {
      int pivot = arr[left];
      int lhs = left + 1;
      int rhs = right - 1;
      while (lhs < rhs) {
        while (arr[lhs] <= pivot && lhs < right - 1) {
          lhs++;
        }

        while (arr[rhs] >= pivot && rhs > left) {
          rhs--;
        }

        if (lhs < rhs) {
          int tmp = arr[lhs];
          arr[lhs] = arr[rhs];
          arr[rhs] = tmp;
        }
      }

      if (arr[lhs] < pivot) {
        arr[left] = arr[lhs];
        arr[lhs] = pivot;
      }

      quickSort(arr, left, lhs);
      quickSort(arr, lhs, right);

    }
  }
}
