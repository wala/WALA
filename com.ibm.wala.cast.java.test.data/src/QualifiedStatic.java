public class QualifiedStatic {
    public static void main(String[] args) {
	QualifiedStatic qs= new QualifiedStatic();
	FooQ fq= new FooQ();
	int x = FooQ.value;
    }
}
class FooQ {
    static int value= 0;
}
