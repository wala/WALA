public class LocalClass {
    public static void main(String[] args) {
	final Integer base = new Integer(6);

	class Foo {
	    int value;
	    public Foo(int v) { value= v; }
	    public int getValue() { return value; }
	    public int getValueBase() { return value - base.intValue(); }
	}
	Foo f= new Foo(3);

	System.out.println(f.getValue());
	System.out.println(f.getValueBase());

	(new LocalClass()).method();
    }

    public void method() {
	final Integer base = new Integer(6);

	class Foo {
	    int value;
	    public Foo(int v) { value= v; }
	    public int getValue() { return value; }
	    public int getValueBase() { return value - base.intValue(); }
	}
	Foo f= new Foo(3);

	System.out.println(f.getValue());
	System.out.println(f.getValueBase());
    }
}
