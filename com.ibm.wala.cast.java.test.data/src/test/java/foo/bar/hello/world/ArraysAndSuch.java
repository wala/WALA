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

public class ArraysAndSuch {

	public static void main(String args[]) {
		ArraysAndSuch.main();
	}
	public static void main() {
		Object o1 = null;
		Object[] os1 = new Object[] { null, o1, null };
		Object[] os2 = { null };
		os1 = new Object[] { null };
		os1 = new String[][] { { null, null }, { null} };
		os1 = new Object[][] { { null, o1 }, { null}, {os2}, {null,os1,null} };
		System.out.println(os1[1]);
		os1[1] = null;

		os1.clone();
		if ( os1.equals(os2) ) {
			Class<? extends Object[]> x = os1.getClass();
			os1.notify();
			os1.toString();
			try {
				x.getClasses().wait(x.getClasses().length,o1.hashCode());
				os1.wait(os1.length);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		float x[] = new float[4+5];
		int[][][][] y = new int[2][x.length][1][1+1];
		int z[] = new int[] { 2+3, 4+3 };
		boolean[] a = new boolean[] { };
		Object b = new String[] { };
		Object c[] = new String[] {};
		String d[] = new String[3];
	}
	
	
}
