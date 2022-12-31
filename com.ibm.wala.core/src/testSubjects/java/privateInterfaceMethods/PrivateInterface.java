package privateInterfaceMethods;

public interface PrivateInterface {
  default void RetT(Object input) {
    GetT(input);
  }

  private Object GetT(Object input) {
    return input;
  }
}
