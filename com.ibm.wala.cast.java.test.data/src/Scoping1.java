public class Scoping1 {
    public static void main(String[] args) {
	Scoping1 s1= new Scoping1();
	{
	    int x= 5;
	    System.out.println(x);
	}
	{
	    double x= 3.14;
	    System.out.println(x);
	}
    }
}