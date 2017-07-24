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

public class MethodMadness {
	public static int s = 12345;
	public int x = 100000;
	
	public MethodMadness() {
		System.out.println("constructor");
	}
	public MethodMadness(String ss) {
		System.out.println("hello world"+ ss);
	}
	
	public static void staticTest() {
		System.out.println("staticTest");
	}
	protected int protectedInteger() {
		this.s = 5;
		new MethodMadness("thrownaway").staticTest(); // MethodMadness object evaluated but thrown away
		new MethodMadness("thrownaway").s++; // MethodMadness object evaluated but thrown away
		staticTest();
		
		MethodMadness nullmm = null;
		System.out.println("s from nullmm: "+nullmm.s); // static refs cannot cause NPEs
		
		return 6 + x;
	}
	protected int protectedInteger2() {
		return 66 + x;
	}
	
	void defaultVoid() {
		if ( true )
			return;
		return;
	}
	
	private void privateVoid() {
		System.out.println("privateVoid "+x);
	}
	protected void protectedVoid() {
		System.out.println("protectedVoid "+x);
	}
	
	private int privateInteger() {
		return 7 + x;
	}
	private int privateInteger2() {
		return 77 + x;
	}
	
	class Inner extends MethodMadness {
		public Inner() {
			x = 200000;
		}

		private int privateInteger() {
			return 13 + x;
		}
		
    @Override
    protected int protectedInteger() {
			return 233 + x;
		}
		
		@SuppressWarnings("static-access")
		public void hello() {
			System.out.println(privateInteger()); // inner function, inner this, 200013
			System.out.println(MethodMadness.this.privateInteger()); // outer function, outer this, 100007 
			System.out.println(privateInteger2()); // outer function, outer this, 200077 // WRONG IN POLYGLOT (private)
			System.out.println(protectedInteger()); // inner function, inner this, 200233 
			System.out.println(MethodMadness.this.protectedInteger()); // outer function, outer this 
			System.out.println(protectedInteger2()); // outer function, inner this, 200066 // the interesting one 
			privateVoid(); // outer function, outer this, 100000 // WRONG IN POLYGLOT (private)
			protectedVoid(); // outer function, inner this, 200000
			
			defaultVoid();
			
			staticTest();
			this.staticTest();
			MethodMadness.this.staticTest();
		}
	}
	
	public static void main(String args[]) {
		new MethodMadness().new Inner().hello();
	}
}
