package privateInterfaceMethods;

public interface PrivateInterface {//called RetT and GetT b/c it used to type T return/input
  default <Object> void RetT(Object input) {
    GetT(input);
  }

  private <Object> Object GetT(Object input) {
    return input;
  }
}
