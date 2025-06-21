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
package javaonepointfive;
// what IS illegal and we don't have to worry about:
//  "x instanceof E"
//  y = new Q();

interface IGeneric<E> {
	E foo();

	E bar(E x, E y);
}

// Y: "implements IGeneric" (no <E>)
// TOTRY: two arguments
class ConcreteGeneric<Q> implements IGeneric<Q> {
	Q x;

	
  @Override
  public Q bar(Q a, Q b) {
		x = a;
		if (b.hashCode() == a.hashCode() || b.toString().equals(a.toString()))
			return a;
		return b;
	}

	
  @Override
  public Q foo() {
		return x;
	}
}

class ConcreteGeneric2<Q> extends ConcreteGeneric<Q> {
	Q y;
	public void setFoo(Q a) {
		y = a;
	}
	
  @Override
  public Q foo() {
		return y;
	}
}

class MyGeneric<A extends Object, B extends IGeneric<A>> {
	A a;
	B b; // TODO: check field type
	public MyGeneric(A a, B b) { // TODO: check parameter type
		this.a = a;
		this.b = b;
	}
	public A doFoo() { // TODO: check return value type
		return b.foo();
	}
	public B getB() {
		return b;
	}
}

public class CustomGenericsAndFields {
	static ConcreteGeneric2<String> cg2 = new ConcreteGeneric2<>();

	static public ConcreteGeneric2<String> cg2WithSideEffects() {
		System.out.println("look at me! I'm a side effect!");
		return cg2;
	}

	public static void main(String args[]) {
		(new CustomGenericsAndFields()).doit();
	}
	
	private void doit() {
		// Simple: concrete generic
		
		ConcreteGeneric<String> absinthe = new ConcreteGeneric<>();
		IGeneric<String> rye = absinthe;
		String foo = rye.bar("hello", "world");
		System.out.println(absinthe.foo() + foo);

		/////////////////////////////

		String thrownaway = cg2.bar("a","b");
		cg2.setFoo("real one");
		MyGeneric<String,ConcreteGeneric2<String>> mygeneric = new MyGeneric<>("useless",cg2);
		String x = mygeneric.doFoo();
		System.out.println(x);
		String y = cg2.x;
		System.out.println(mygeneric.getB().y);
		System.out.println(mygeneric.b.y); // TODO: fields are going to be a pain... watch out for Lvalues in context?
		cg2.x = null;
		cg2.x = "hello";

//		mygeneric.getB().y+="hey"; // TODO: this is going to be a MAJOR pain...
		String real_oneheyya = (((cg2WithSideEffects().y))+="hey")+"ya"; // TODO: this is going to be a MAJOR pain...
		System.out.println(real_oneheyya);
	}
}
