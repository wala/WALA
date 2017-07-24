/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * WALA JDT Frontend is Copyright (c) 2008 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package foo.bar.hello.world;

class Temp {
	
}
public class InnerClasses extends Temp {
	
	Object supportLocalBusiness() {
		final int x = 54;
		class FooBar {
      @Override
      public int hashCode() { return x; }
		}
		return new FooBar();
	}
	
	static Object anonymousCoward() {
		final int x = 5;
		return new Object() {
      @Override
      public int hashCode() { return x; }
		};
	}
	
	static String xxxx = "odd";
	
	public static void main(String args[]) {
		new Outie("eenie").new Innie().meenie();
		System.out.println(anonymousCoward().hashCode());
		final String xx = "crazy";
		Outie outie = new Outie("weird") {
      @Override
      public String toString() {
				return "bogus" + x + xx + xxxx;
			}
		};
		System.out.println(outie);
		
		ABC a = new ABC("hello") { };
		System.out.println(new InnerClasses().supportLocalBusiness().hashCode());
		System.out.println(a);
		
		System.out.println(new SuperEnclosing().new SubEnclosed().hello());
		System.out.println(new SuperEnclosing().new SubEnclosed().seVar);
		System.out.println(new SuperEnclosing().new SubEnclosed().hello2());
		System.out.println(new SuperEnclosing().new SubEnclosed().seVar2);
		
		SuperEnclosing2 se2 = new SuperEnclosing2();
		SuperEnclosing2.SubEnclosed2 sub2 = se2.new SubEnclosed2();
		System.out.println(sub2.hello()); //13 
		sub2.setSEVar();
		System.out.println(sub2.hello()); //still 13
		se2.setSEVar();
		System.out.println(sub2.hello()); //1001
		
		int foo = 12;
		foo++;
		--foo;
	}
}

class ABC { ABC(Object x) {} } 

class Outie {
	String x;
	
	Outie(String s) {
		x = s;
	}
	
	class Innie {
		public void meenie() {
			System.out.println(x);
		}
	}
}

class SuperEnclosing {
	int seVar = 6;
	int seVar2 = 10;
	class SubEnclosed extends SuperEnclosing {
		int seVar2 = 11;
		SubEnclosed() {
			this.seVar = 5;
			SuperEnclosing.this.seVar = 7;
			this.seVar2 = 12;
			SuperEnclosing.this.seVar2 = 13;
		}
		int hello() {
			return seVar; // this is ours from SuperEnclosing, not the enlosing SuperEnclosing's
			// so even though the variable is defined in SuperEnclosing, this is NOT the same as
			// SuperEnclosing.this.x
		}
		int helloAgain() {
			return SubEnclosed.this.seVar; // this is ours from SuprEnclosing, not the enclosing SuperEnclosing's
		}
		int hello2() {
			return seVar2;
		}
	}
}

class SuperEnclosing2 {
	private int seVar = 6;
	public void setSEVar() {
		seVar= 1001;
	}
	class SubEnclosed2 extends SuperEnclosing2 {
		SubEnclosed2() {
			// this.seVar = 5; // illegal 
			SuperEnclosing2.this.seVar += -(-(+7)); // makes 13
		}
		int hello() {
			return seVar; // since seVar is private, this now refers to the enclosing one
			// PROOF: callind setSEVar() on the SubEnclosed2 above does nothing.
		}
	}
}
