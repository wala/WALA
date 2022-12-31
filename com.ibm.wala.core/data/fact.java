class fact {

  static int fact(int n) {
    if (n < 2) {
      return 1;
    } else {
      return n * fact(n - 1);
    }
  }

  public static void main(String... args) {
    fact(6);
  }
}
