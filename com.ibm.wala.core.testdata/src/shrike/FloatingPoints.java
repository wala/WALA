package shrike;

public class FloatingPoints {

  public static void main(String[] args) {
    doubble();
    floatt();
  }

  public static void doubble() {
    double pi = Math.PI;
    double added = pi + 1.337;
    System.out.println(added);
  }

  public static void floatt() {
    float f = 42.1337f;
    double added = f + 1.337;
    System.out.println(added);
  }
}
