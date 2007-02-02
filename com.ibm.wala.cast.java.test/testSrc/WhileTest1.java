public class WhileTest1 {
    public static void main(String[] args) {
	WhileTest1 wt1= new WhileTest1();
	int x= 235834;
	boolean stop= false;

	while (!stop) {
	    x += 3;
	    x ^= 0xAAAA5555;
	    stop= (x & 0x1248) != 0;
	}

	while (!stop) {
	    x += 3;
	    if (x < 7) continue;
	    x ^= 0xAAAA5555;
	    stop= (x & 0x1248) != 0;
	}
    }
}
