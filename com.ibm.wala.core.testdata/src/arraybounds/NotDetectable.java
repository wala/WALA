package arraybounds;

/**
 * 
 * All array accesses in the following class are unnecessary but they will not
 * be detected correctly by the array bounds analysis.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class NotDetectable {
  private final int[] memberArr = new int[5];

  /**
   * Member calls are not supported. See {@link Detectable#memberLocalGet(int)}
   * for workaround.
   * 
   * @param i
   * @return memberArr[i]
   */
  public int memberGet(int i) {
    if (i >= 0 && i < memberArr.length) {
      return memberArr[i];
    } else {
      throw new IllegalArgumentException();
    }
  }

  private int getLength() {
    return memberArr.length;
  }

  /**
   * Interprocedural analysis is not supported.
   * 
   * @param i
   * @return memberArr[i]
   */
  public int interproceduralGet(int i) {
    if (i >= 0 && i < getLength()) {
      return memberArr[i];
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * This example does not work: We know 5 &gt; 3 and sometimes length &gt; 5 &gt; 3. In
   * case of variables this conditional relation is resolved by introducing pi
   * nodes. For constants pi nodes can be generated, but the pi variables will
   * not be used (maybe due to constant propagation?). Additionally 5 != 3, so
   * even if we would use pi-variables for 5, there would be no relation to 3: 0
   * -(5)-&gt; 5, 5 -(-5)-&gt; 0, {5,length} -(0)-&gt; 5', 0 -(3)-&gt; 3, 3 -(-3)-&gt; 0 Given
   * the inequality graph above, we know that 5,5',3 are larger than 0 and 5
   * larger 3 and length is larger than 5', but not 5' larger than 3. Which is
   * not always the case in general anyway.
   * 
   * This may be implemented by replacing each use of a constant dominated by a
   * definition of a pi of a constant, with a fresh variable, that is connected
   * to the inequality graph accordingly.
   * 
   * For a workaround see {@link Detectable#nonFinalConstant(int[])}
   * 
   * 
   * 
   * @param arr
   * @return arr[3]
   */
  public int constants(int[] arr) {
    if (arr.length > 5) {
      return arr[3];
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * As the variable i is final, constant propagation will prevent the detection
   * (See also {@link NotDetectable#constants(int[])}), for a working example
   * see {@link Detectable#nonFinalConstant(int[])}.
   * 
   * @param arr
   * @return arr[3]
   */
  public int dueToConstantPropagation(int[] arr) {
    final int i = 3;
    if (i < arr.length) {
      return arr[i];
    } else {
      throw new IllegalArgumentException();
    }
  }

  public int indirectComparison(int[] arr) {
    int i = 3;
    int e = i + 1;
    if (e < arr.length) {
      return arr[i];
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Neither modulo, multiplication or division with constants will work.
   * 
   * Note: Any operation can be performed BEFORE comparing a variable to the
   * array length.
   * 
   * @param arr
   * @param i
   * @return arr[i]
   */
  public int modulo(int[] arr, int i) {
    if (0 <= i && i < arr.length) {
      return arr[i % 2];
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Neither subtraction of variables nor inverting variables will work.
   * 
   * Note: Any operation can be performed BEFORE comparing a variable to the
   * array length.
   * 
   * @param arr
   * @param i
   * @return arr[i]
   */
  public int variableSubtraction(int[] arr, int i) {
    if (0 <= i && i < arr.length) {
      int i2 = 0 - i;
      int i3 = -i2;
      return arr[i3];
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * The analysis can not detect, that forward is false every other iteration.
   * So there is a positive and a negative loop. The positive loop is bound by
   * the loop condition, while the negative loop is unbound, so index might be
   * smaller than zero. This should result in the lower bound check beeing
   * necessary.
   * 
   * @param arr
   * @return sum of all elements in arr
   */
  public int nonMonotounous(int arr[]) {
    int index = 0;
    int sum = 0;
    boolean forward = true;
    while (index < arr.length) {
      sum += arr[index];
      if (forward) {
        index += 2;
      } else {
        index -= 1;
      }
      forward = !forward;
    }
    return sum;
  }
  
  /**
   * Multidimensional Arrays are not supported yet.
   */  
  public int multiDimensional(){
    int arr[][] = new int[5][10];
    return arr[2][3];
  }
}
