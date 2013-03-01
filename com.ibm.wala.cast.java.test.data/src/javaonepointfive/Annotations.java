package javaonepointfive;

@interface TestAnnotation {
	String doSomething();
	int count(); 
	String date();
	int[] stuff();
}

@interface AnotherTestAnnotation {
	String value();
}

@AnotherTestAnnotation("string")
@TestAnnotation (doSomething="The class", count=-1, date="09-09-2001", stuff={0,1})
public class Annotations {
	
	@TestAnnotation (doSomething="What to do", count=1, date="09-09-2005", stuff={})
    public int mymethod() {
		@AnotherTestAnnotation("i")
		int i = 0;
		return i;
	}

	@TestAnnotation (doSomething="What not to do", count=0, date="12-14-2010", stuff={1,2,3,4,5})
	public static void main(String[] args) {
		(new Annotations()).mymethod();
	}
}
