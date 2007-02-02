public class StaticNesting {
    public static void main(String[] args) {
	StaticNesting sn= new StaticNesting();
	WhatsIt w= new WhatsIt();
    }
    static class WhatsIt {
	private int value;
	public WhatsIt() {
	    value= 0;
	}
    }
}