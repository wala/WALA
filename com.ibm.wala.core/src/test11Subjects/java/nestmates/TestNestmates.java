package nestmates;

public class TestNestmates {
  public void main(String[] args) {
    Outer orig = new Outer();
    Outer.Inner tester = orig.new Inner();

    tester.triple();
  }
}
