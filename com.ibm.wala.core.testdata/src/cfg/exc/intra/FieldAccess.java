/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package cfg.exc.intra;


public class FieldAccess {
  
  
  static B testIf(boolean unknown, B b1, B b2) {
    b1.f = 42;
    b2.f = 17;

    B b3;
    if (unknown) {
      b3 = b1;
    } else {
      b3 = b2;
    }
    
    return b3;
  }

  static B testIf2(boolean unknown, B b1, B b2) {
    b1.f = 42;

    B b3;
    if (unknown) {
      b3 = b1;
    } else {
      b3 = b2;
    }
    
    return b3;
  }
  
  static B testIf3(boolean unknown, B b1) {
    if (unknown) {
      b1.f = 42;
    } else {
      System.out.println("rofl");
    }
    
    return b1;
  }
  
  static B testWhile(boolean unknown, B b1) {
    b1.f = 42;

    B b3 = null;
    while (unknown) {
      b3 = b1;
    }
    
    return b3;
  }
  
  static B testWhile2(boolean unknown, B b1) {
    b1.f = 42;

    B b3 = new B();
    b3.f = 17;
    
    while (unknown) {
      b3 = b1;
    }
    
    return b3;
  }

	public static void main(String[] args) {

		B b1 = new B();
		B b2 = new B();
		final boolean unknown = (args.length == 0);

		testIf(unknown, b1, b2);
	}
}
