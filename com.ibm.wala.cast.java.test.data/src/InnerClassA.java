/******************************************************************************
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
// other stranger test cases
//
// combininations of: 
//		o.new() form
//      function calls
//      getting enclosings from one AND multiple levels up
//      new Foo() which requires an enclosing instruction before (ie calling new Inner() from ReallyInner()
//      invariants and non-invariants (immediate 'new' in that function)
//      subclasses

public class InnerClassA {
	int a_x;

	public static void main(String args[]) {
		// prints out 5 5 9 7 5 5 7 5 7 5
		InnerClassA a = new InnerClassA();
		AA aa = a.new AA();
		AB ab = aa.makeAB();
		a.a_x = 5;

		// tests
		int myx = ab.getA_X_from_AB();
		System.out.println(myx); // 5
		int myx2 = ab.getA_X_thru_AB();
		System.out.println(myx2); // 5
		
		aa.doSomeCrazyStuff();
	}

	public int getA_X() {
		return a_x;
	}
	
	class AA {
		int a2a_var;
		
		public AB makeAB() {
			return new AB();
		}
		
		public void doSomeCrazyStuff() {
			AB ab = new AB();
			AB.ABSubA absuba = ab.new ABSubA();
			absuba.aba_x = 7;
			@SuppressWarnings("unused")
			AB.ABA.ABAA abaa2 = ab.new ABA().new ABAA(); // just used to add ABA instance key in ABAA.getABA_X()
			
			AB.ABA aba = ab.new ABA();
			aba.aba_x = 9;
			AB.ABA.ABAB abab = aba.new ABAB();
			System.out.println(abab.getABA_X()); // 9
			
			AB.ABA.ABAA abaa = absuba.new ABAA();
			int myaba_x = abaa.getABA_X();
			int mya_x = abaa.getA_X();
			System.out.println(myaba_x); // 7
			System.out.println(mya_x); // 5
			
			
			doMoreWithABSubA(absuba);
		}

		private void doMoreWithABSubA(InnerClassA.AB.ABSubA absuba) {
			System.out.println(absuba.getA_X()); // 5
			
			AB.ABSubA.ABSubAA absubaa = absuba.new ABSubAA();
			int myaba_x2 = absubaa.getABA_X();
			int mya_x2 = absubaa.getA_X();
			System.out.println(myaba_x2); // 7
			System.out.println(mya_x2); // 5
			// TODO Auto-generated method stub

			AB.ABA.ABAA abaa = absubaa.makeABAA();
			int myaba_x3 = abaa.getABA_X();
			int mya_x3 = abaa.getA_X();
			System.out.println(myaba_x3); // 7
			System.out.println(mya_x3); // 5

		}
	}

	class AB {
		public int getA_X_from_AB() {
			return a_x; // CHECK enclosing is an A
		}

		public int getA_X_thru_AB() {
			return getA_X(); // CHECK enclosing is an A
		}
		
		class ABA {
			int aba_x;
			class ABAA {
				int getABA_X() {
					return aba_x; // CHECK enclosing is an ABA or ABSubA
				}
				int getA_X() {
					return a_x; // CHECK enclosing is an A
				}
			}
			class ABAB {
				int getABA_X() {
					return aba_x; // CHECK enclosing is an ABA
				}
			}
		}
		
		class ABSubA extends ABA {
			class ABSubAA {
				int getABA_X() {
					return aba_x; // CHECK enclosing is an ABSubA
				}
				int getA_X() {
					return a_x; // CHECK enclosing is an A
				}
				ABA.ABAA makeABAA() {
					return new ABAA(); // this new instruction requires us to know that ABSubA is a subclass of ABA.
									   // thus when we call getABA_X() on the result, it will need to find a EORK(this site,class ABA) -> THIS (of type ABSubAA) 
				}
			}
			int getA_X() {
				return a_x; // CHECK enclosing is an A
			}
		}
	}
}
