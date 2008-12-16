
public class WelcomeInitializers {
	int x;
	int y = 6;
	static int sX;
	static int sY = 6;
	
	{
		x = 7 / 7;
	}
	
	static { 
		sX = 9 / 3;
	}
	
	public WelcomeInitializers() {
		
	}
	public void hey() {}
	
	public static void main(String args[]) {
		new WelcomeInitializers().hey();
	}
}
