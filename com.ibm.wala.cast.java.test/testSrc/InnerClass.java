public class InnerClass {
    public static void main(String[] args) {
      (new InnerClass()).method();
    }

    public int fromInner(int v) {
      return v + 1;
    }

    public void method() {
	WhatsIt w= new WhatsIt();
    }

    class WhatsIt {
	private int value;
	public WhatsIt() {
	  value= 0;
	  fromInner(value);
	}
    }
}
