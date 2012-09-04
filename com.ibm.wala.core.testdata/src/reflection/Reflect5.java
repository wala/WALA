package reflection;


public class Reflect5 {

	public static void main(String[] args) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
		Class c = Class.forName("reflection.Reflect5$A");
		A h = (A) c.newInstance();
		System.out.println(h.toString());
	}
	
	public static class A {
		private A() {
		}
		public String toString() {
			return "Instance of A";
		}
	}
}
