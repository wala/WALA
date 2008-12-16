public class Finally1 {
    public static void main(String[] args) throws BadLanguageExceptionF1 {
	Finally1 f1= new Finally1();
	try {
	    FooF1 f = new FooF1();

	    f.bar();
	} finally {
	    System.out.println("blah");
	}
	System.out.println("feep");
    }
}
class FooF1 {
    public void bar() throws BadLanguageExceptionF1 {
	if (true)
	    throw new BadLanguageExceptionF1();
	System.out.println("feh");
    }
}
class BadLanguageExceptionF1 extends Exception {
    public BadLanguageExceptionF1() {
	super("Try using a real language, like Perl");
    }
}
