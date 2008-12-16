public class Inheritance1 {
    public static void main(String[] args) {
	Inheritance1 ih1= new Inheritance1();
	Base b1 = new Base();
	Base b2 = new Derived();

	b1.foo();
	b2.foo();
	b1.bar(3);
	b2.bar(5);
    }
}
class Base {
    public void foo() {
	int i= 0;
    }
    public String bar(int x) {
	return Integer.toOctalString(x);
    }
}
class Derived extends Base {
    public void foo() {
	super.foo();
    }
    public String bar(int x) {
	return Integer.toHexString(x);
    }
}