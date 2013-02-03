package javaonepointfive;

@interface TestAnnotation {
	String doSomething();
	int count(); 
	String date();
}

@TestAnnotation (doSomething="The class", count=-1, date="09-09-2001")
public class Annotations {
	
	@TestAnnotation (doSomething="What to do", count=1, date="09-09-2005")
    public void mymethod() {
		// do nothing	
	}

	@TestAnnotation (doSomething="What not to do", count=0, date="12-14-2010")
	public static void main(String[] args) {
		(new Annotations()).mymethod();
	}
}
