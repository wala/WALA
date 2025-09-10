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

class Alef {
	void foo(String... args) {
		System.out.println("var args not overridden");
	}
}

class Bet extends Alef {
	void foo(String a, String b) {
		System.out.println("non-varargs overrides varargs");
	}
}

class Alpha {
	void foo(String a, String b) {
		System.out.println("non-varargs not overridden");
	}
}

class Beta extends Alpha {
	void foo(String... args) {
		System.out.println("varargs overrides non-varargs");
	}
}


////

class VarityTestSuper {
	void bar(String... args) {}
}

class VarityTestSub extends VarityTestSuper {
	
  @Override
  void bar(String... args) {}
}

public class VarargsOverriding {
	public static void main(String args[]) {
		(new VarargsOverriding()).doit();
	}
	
	private void doit() {
		Bet b = new Bet();
		Alef a = b;
		a.foo("hello", "world");
		b.foo("hello", "world");

		Beta bb = new Beta();
		Alpha aa = bb;
		aa.foo("hello", "world");
		bb.foo("hello", "world");
		bb.foo("hello", "world", "and", "more");

		VarityTestSuper x = new VarityTestSuper();
		x.bar("Hello", "world", "howareya");
		x = new VarityTestSub();
		x.bar("Hello", "world", "howareya");
	}
}
