public class InheritedField {
    public static void main(String[] args) {
	InheritedField if1= new InheritedField();
	B b = new B();

	b.foo();
	b.bar();
    }
}
class A {
    protected int value;
}
class B extends A {
    public void foo() {
	value = 10;
    }
    public void bar() {
	this.value *= 2;
    }
}
