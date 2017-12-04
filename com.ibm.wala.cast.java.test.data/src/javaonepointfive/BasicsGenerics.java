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
import java.util.Iterator;

public class BasicsGenerics {
	
	static ArrayList<String> strs = new ArrayList<>();
	static ArrayList<Integer> ints = new ArrayList<>();
	
	public BasicsGenerics() {
		strs.add("Coucou, monde!");
	}

	public BasicsGenerics(String s) {
		strs.add(s);
	}
	
	public static void main(String args[]) {
		BasicsGenerics a = new BasicsGenerics();
		String frenchy = a.part1();
//		
//		
		String s = "mondo";
		String sicilian = new BasicsGenerics("ciao "+s).part2();
//		
		System.out.println(frenchy);
		System.out.println(sicilian);
		strs.add("hello");
		ints.add(new Integer(3));
		
		String qqq;
		
		for (Iterator iter = ((Iterable)ints).iterator(); iter.hasNext(); iter.next());
		
		for (Iterator<String> iter = strs.iterator(); iter.hasNext();) {
			qqq = iter.next();
			System.out.println(qqq);
		}
		
		Iterable s1 = strs;
		for (Iterator itertmp = s1.iterator(); itertmp.hasNext();) {
			String rrr = (String) itertmp.next();
			{
				System.out.println("la vida pasaba y sentia " + rrr);
			}
		}
		
		for (String rrr: strs) {
			System.out.println("la vida pasaba y sentia " + rrr);
		}

		for (String rrr: strs) {
			System.out.println("la vida pasaba y sentia " + rrr);
		}

		for ( String x: makeArray() )
			System.out.println(x);
//		
//		System.out.println("---break time---");
//		for ( int i = 0; i < makeArray().length; i++ )
//			System.out.println(makeArray()[i]);
	}

	public static String[] makeArray() {
		String[] hey = {"hey","whats","up"};
		System.out.println("give a hoot");
		return hey;
	}
	
	public String part1() {
		return strs.get(0);
	}

	public String part2() {
		return strs.get(0);
	}
}
