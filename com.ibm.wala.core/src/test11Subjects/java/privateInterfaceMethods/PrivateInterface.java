package privateInterfaceMethods;

public interface PrivateInterface { // called RetT and GetT b/c it used to type T return/input
  default void RetT(Object input) {
    GetT(input);
  }

  private Object GetT(Object input) {
    return input;
  }
}
