public class Simple1 {
    private int value;
    private final float fval = 3.14F;
    private float fval1 = 3.2F;
    public Simple1(int x) {
	value = x;
    }
    public Simple1() {
	this(0);
    }
    public static void doStuff(int N) {
	int prod = 1;
	for(int j=0; j < N; j++)
	    prod *= j;
    }
    public static void main(String[] args) {
	int sum= 0;
	for(int i=0; i < 10; i++) {
	    sum += i;
	}
	Simple1.doStuff(sum);
	Simple1 s = new Simple1();
	s.instanceMethod1();
    }
    public void instanceMethod1() {
	instanceMethod2();
    }
    public float instanceMethod2() {
        return fval * fval1;
    }
}
