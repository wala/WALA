public class InterfaceTest1 {
    public static void main(String[] args) {
	InterfaceTest1 it= new InterfaceTest1();
	IFoo foo = new Foo('a');
	char ch2 = foo.getValue();
    }
}
interface IFoo {
    char getValue();
}
class Foo implements IFoo {
    private char fValue;
    public Foo(char ch) {
	fValue= ch;
    }
    public char getValue() {
	return fValue;
    }
}
