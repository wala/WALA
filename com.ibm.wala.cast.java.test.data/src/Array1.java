public class Array1 {
    public static void main(String[] args) {
	Array1 f= new Array1();
	f.foo();
    }
    public void foo() {
	int[] ary = new int[5];

	for(int i= 0; i < ary.length; i++) {
	    ary[i]= i;
	}

	int sum = 0;

	for(int j= 0; j < ary.length; j++) {
	    sum += ary[j];
	}
    }
}