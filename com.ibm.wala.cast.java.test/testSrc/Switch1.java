public class Switch1 {
    public static void main(String[] args) {
	Switch1 s1= new Switch1();
	int x= Integer.parseInt(args[0]);
	char ch;
	switch(x) {
	    case 0: ch=Character.forDigit(Integer.parseInt(args[1]), 10); break;
	    case 1: ch=Character.forDigit(Integer.parseInt(args[2]), 10); break;
	    case 2: ch=Character.forDigit(Integer.parseInt(args[3]), 10); break;
	    case 3: ch=Character.forDigit(Integer.parseInt(args[4]), 10); break;
	    default: ch= '?'; break;
	}
	System.out.println(ch);
    }
}