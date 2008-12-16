
interface ISimpleCalls {
	public void helloWorld();
}
public class SimpleCalls implements ISimpleCalls {
	public void helloWorld() {
		System.out.println("hello world!");
	}
	public int anotherCall() {
		this.helloWorld();
		((this)).helloWorld();
		System.out.println("another call");
		return 5;
	}
	public static void main (String args[]) {
		SimpleCalls sc = new SimpleCalls();
		ISimpleCalls isc = sc;
		isc.helloWorld();
		int y = sc.anotherCall();
		y = y + y;
	}
}
