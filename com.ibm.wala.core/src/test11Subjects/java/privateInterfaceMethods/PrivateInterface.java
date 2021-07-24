package privateInterfaceMethods;

public interface PrivateInterface {
  default <T> void RetT(T input) {
    System.out.println(GetT(input));
  }

  private <T> T GetT(T input) {
    return input;
  }
}
