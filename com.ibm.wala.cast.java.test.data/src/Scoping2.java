public class Scoping2 {
    public static void main(String[] args) {
	Scoping2 s2= new Scoping2();
	{
	    final int x= 5;
	    System.out.println(x);
	    (new Object() {
		public void foo() {
		    System.out.println("x = " + x);
		}
	    }).foo();
	}
	{
	    double x= 3.14;
	    System.out.println(x);
	}
    }
}