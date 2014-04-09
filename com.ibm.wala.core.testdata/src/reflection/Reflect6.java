package reflection;


public class Reflect6 {

	public static void main(String[] args) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
		Class c = Class.forName("reflection.Reflect6$A");
		A h = (A) c.newInstance();
		System.out.println(h.toString());
	}
	
	public static class A {
		private A(int i) {
		}
		public String toString() {
			return "Instance of A";
		}
	}
}
