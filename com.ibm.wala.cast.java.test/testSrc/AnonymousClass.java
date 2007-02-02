public class AnonymousClass {
    private interface Foo {
      public int getValue();
      public int getValueBase();
    }

    public static void main(String[] args) {
	final Integer base = new Integer(6);

	Foo f= new Foo() {
	    int value = 3;
	    public int getValue() { return value; }
	    public int getValueBase() { return value - base.intValue(); }
	};

	System.out.println(f.getValue());
	System.out.println(f.getValueBase());

	(new AnonymousClass()).method();
    }

    public void method() {
	final Integer base = new Integer(7);

	abstract class FooImpl implements Foo {
	    int y;

	    public abstract int getValue();

	    FooImpl(int _y) {
	      y = _y;
	    }

	    public int getValueBase() { 
	      return y + getValue() - base.intValue(); 
	    }
	}

	Foo f= new FooImpl(-4) {
	  public int getValue() { return 7; }
	};

	System.out.println(f.getValue());
	System.out.println(f.getValueBase());
    }
}
