

public class FunkySupers {
	int y;
	int funky(FunkySupers fs) {
		return 5;
	}
	
	public static void main(String args[]) {
		new SubFunkySupers().funky(new FunkySupers());
	}
}

class SubFunkySupers extends FunkySupers {
	int funky(FunkySupers fs) {
		SubFunkySupers.super.funky(fs);
		SubFunkySupers.this.funky(fs);
		SubFunkySupers.this.y = 7;
		SubFunkySupers.super.y = 7;
		super.y = 7;
		super.funky(fs);
		return 6;
	}
}

//class EE { class X {} }
//class Y extends EE.X { Y(EE e) { e.super(); } }
// DOESNT WORK IN POLYGLOT!!!