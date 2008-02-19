public class InnerClassSuper {
	int x = 5;
	class SuperOuter {
		public void test() {
			System.out.println(x);
		}
	}
	public static void main(String args[]) {
		new Sub().new SubInner();
	}
}
class Sub extends InnerClassSuper {
	class SubInner {
		public SubInner() {
			InnerClassSuper.SuperOuter so = new InnerClassSuper.SuperOuter();
			so.test();
		}
	}
}