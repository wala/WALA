import java.io.IOException;

public class Finally2 {
    public static void main(String[] args) throws IOException {
	try {
	    FooF2 f = new FooF2();

	    f.bar();
	    f.bletch();
	} catch (BadLanguageExceptionF2 e) {
	    e.printStackTrace();
	} finally {
	    System.out.println("blah");
	}
    }
}
class FooF2 {
    public void bar() throws BadLanguageExceptionF2 {
	throw new BadLanguageExceptionF2();
    }
    public void bletch() throws IOException {
	throw new IOException("Burp!");
    }
}
class BadLanguageExceptionF2 extends Exception {
    public BadLanguageExceptionF2() {
	super("Try using a real language, like Perl");
    }
}
