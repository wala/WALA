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

import java.util.ArrayList;

public class Wildcards {
	public void printCollection(ArrayList<?> c) {
		// for (Object e : c) {

		// for (Iterator tmp = c.iterator(); tmp.hasNext(); ) {
		// Object e = tmp.next();

		Object e = c.get(0);
		System.out.println(e);
		// }
	}
	public void printCollection1(ArrayList<? extends Object> c) {
		Object e = c.get(0);
		System.out.println(e);
	}
	public void printCollection2(ArrayList<? extends String> c) {
		String e = c.get(0);
		System.out.println(e);
	}

	public static void main(String args[]) {
		(new Wildcards()).doit();
	}
	
	private void doit() {
		ArrayList<String> e = new ArrayList<>();
		e.add("hello");
		e.add("goodbye");
		printCollection(e);
		printCollection1(e);
		printCollection2(e);

		ArrayList<Integer> e3 = new ArrayList<>();
		e3.add(new Integer(123));
		e3.add(new Integer(42));
		printCollection(e3);
		printCollection1(e3);
		
	}
}
