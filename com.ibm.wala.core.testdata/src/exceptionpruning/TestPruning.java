package exceptionpruning;

public class TestPruning {
  public void testTryCatchOwnException(int i) {
    try {
      switch (i) {
      case 1:
        invokeSingleThrowOwnException();
        break;
      case 2:
        throw new OwnException();
      case 3:
        invokeSinglePassThrough();
        break;
      case 5:
        invokeSingleRecursive(i);
        break;
      case 4:
        invokeSingleRecursive2(i);
        break;
      }
    } catch (OwnException e) {

    }
  }

  public int testTryCatchImplicitException(int i) {    
    int res = 0;

    try {
      int[] a = new int[]{1,3,4};
      
      switch (i) {
      case 3:
        invokeSingleImplicitException(a);
        break;
      case 4:       
        res = a[5];
      }
    } catch (ArrayIndexOutOfBoundsException e) {

    }
    
    return res;
  }

  public void testTryCatchSuper(int i) {
    try {
      switch (i) {
      case 5:
        invokeSingleThrowOwnException();
        break;
      case 6:
        throw new OwnException();
      case 7:
        invokeAllPassThrough();
        break;
      }
    } catch (RuntimeException e) {

    }
  }

  public int testTryCatchMultipleExceptions(int i) {    
    int res = 0;

    try {
      int[] a = new int[]{1,3,4};
      
      switch (i) {
      case 7:
        invokeAll();
        break;
      case 8:
        throw new OwnException();
      case 10:
        res = a[5];
        break;
      case 11:
        invokeSingleRecursive(i);
        break;
      case 12:
        invokeSingleRecursive2(i);
        break;
      case 13:
        invokeAllPassThrough();
        break;
      }
    } catch (ArrayIndexOutOfBoundsException e) {

    } catch (OwnException e) {

    }    
    return res;
  }

  public void invokeAll() {
    invokeSingleThrowOwnException();   
    invokeSingleImplicitException(null);
  }

  public void invokeAllPassThrough() {
    invokeAll();
  }

  public void invokeSinglePassThrough() {
    invokeSingleThrowOwnException();
  }

  public void invokeSingleThrowOwnException() {
    throw new OwnException();
  }

  public int invokeSingleImplicitException(int[] a) {
    // may throw NullPointerException implicit
    return a[5];
  }
  
  public void invokeSingleRecursive(int i) {
    if (i == 0) {
      throw new OwnException();
    } else {
      invokeSingleRecursive(i - 1);
    }
  }
  
  public void invokeSingleRecursive2(int i) {
    if (i == 0) {
      throw new OwnException();
    } else {
      invokeSingleRecursive2Helper(i - 1);
    }
  }
  
  public void invokeSingleRecursive2Helper(int i) {
    invokeSingleRecursive2(i);
  }  

  public static void main(String[] args) {
    TestPruning test = new TestPruning();
    for (int i = 0; i < 50; i++) {
      test.testTryCatchOwnException(i);
      test.testTryCatchMultipleExceptions(i);
      test.testTryCatchImplicitException(i);
      test.testTryCatchSuper(i);
    }
  }
}
