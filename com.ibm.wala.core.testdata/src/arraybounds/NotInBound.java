package arraybounds;

/**
 * 
 * All array accesses in the following class are necessary and they will be
 * detected correctly by the array bounds analysis.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class NotInBound {
  public int phiOverwrite(int[] arr, boolean condition) {
    if (arr.length > 0) {
      int l = arr.length;
      if (condition) {
        l = 5;
      }

      return arr[l - 1];
    } else {
      throw new IllegalArgumentException();
    }
  }

  public void offByOne(int[] arr) {
    for (int i = 0; i <= arr.length; i++) {
      arr[i] = 0;
    }
  }

  public int unknownLength(int[] arr) {
    return arr[4];
  }

  public int ambiguity(int[] arr1, int[] arr2, int i) {
    int[] arr = arr2;
    if (0 <= i && i < arr.length) {
      arr = arr1;
      return arr[i];
    } else {
      throw new IllegalArgumentException();
    }
  }

  public int innerLoop(int[] arr) {
    int sum = 0;
    int i = 0;
    while (i < arr.length) {
      while (i < 6) {
        i++;
        sum += arr[i];
      }
    }
    return sum;
  }

  public int comparedWrong(int[] arr, int index) {
    if (index > 0 && index > arr.length) {
      return arr[index];
    } else {
      throw new IllegalArgumentException();
    }
  }

  public int nonTrackedOperationIsSafe(int[] arr, int index) {
    if (index > 0 && index > arr.length) {
      return arr[2*index];
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  public int unboundNegativeLoop(int[] arr, int index) {
    int sum = 0;
    for (int i = 5; i < arr.length; i--) {
      sum += arr[i];
    }
    return sum;
  }
}
