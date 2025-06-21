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

public class Varargs {
	public Varargs(int... x) {
		System.out.println(x[0]==x[1]);
	}
	public Varargs() {
		this(1,1);
	}
	
	public static void varargs(int x, int y, Object... foo) {
		System.out.println(x);
		System.out.println(y);
		for (Object o : foo)
			System.out.println(o);

		new Varargs();
	}

	// try overriding a function and causing havoc (i assume it knows at compile time whether to expand varargs)
	public static void varargs(int x, int y, String foo, String xa, String ya, String z) {
		System.out.println("gotcha!");
	}

	public static void main(String args[]) {
		varargs(2, 3, new String[] { "hello", "world" });
		varargs(2, 3, (Object) new String[] { "hello", "world" });
		varargs(2, 3, (Object[]) new String[] { "hello", "world" });
		varargs(2, 3);
		varargs(2, 3, "hi");
		varargs(2, 3, "hello", "there");
		varargs(4, 5, "coucou", "monde", "shalom", "'olam");
	}
}
