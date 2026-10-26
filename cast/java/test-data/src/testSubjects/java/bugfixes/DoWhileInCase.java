package bugfixes;

public class DoWhileInCase {
  static int x = 3;
  public static void main(String[] args) {
    new DoWhileInCase().run(args);
  }
   public void run(String[] args) {
    switch(x) {
    case 1:
      do {
        System.out.println("Problem");
        x ++;
      } while (x < 3);
      break;
    default:
      System.out.println("Default");
    }
  }
}
