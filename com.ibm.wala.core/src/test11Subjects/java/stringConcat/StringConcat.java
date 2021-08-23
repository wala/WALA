package stringConcat;

public class StringConcat {
  String testConcat() {
    String s1 = "thing 1";
    String s2 = "thing 2";

    String s3 = s1 + s2;

    return s3 + "foobar";
  }

  public void main(String[] args) {
    testConcat();
  }
}
