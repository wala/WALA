public class InnerClass {
    public static void main(String[] args) {
	(new InnerClass()).method();
    }

    public void method() {
	WhatsIt w= new WhatsIt();
    }

    class WhatsIt {
	private int value;
	public WhatsIt() {
	    value= 0;
	}
    }
}
