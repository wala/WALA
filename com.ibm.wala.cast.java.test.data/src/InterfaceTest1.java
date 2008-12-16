public class InterfaceTest1 {
    public static void main(String[] args) {
	InterfaceTest1 it= new InterfaceTest1();
	IFoo foo = new FooIT1('a');
	char ch2 = foo.getValue();
    }
}

interface IFoo {
    char getValue();
}

class FooIT1 implements IFoo {
    private char fValue;
    public FooIT1(char ch) {
	fValue= ch;
    }
    public char getValue() {
	return fValue;
    }
}
