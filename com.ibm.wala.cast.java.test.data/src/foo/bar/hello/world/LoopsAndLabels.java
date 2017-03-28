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

public class LoopsAndLabels {
	@SuppressWarnings({ "cast", "unused" })
	public static void main(String args[]) {

		aaa: do {
			while (null instanceof String) {
				String x = (String) null;
				Object a = (Object) x;
				Object b = (Object) "hello";
				x = (String) a;
				x = "hello";
				x = (String) b.toString();
				x = ((String)((Object) b.toString()));
				x = (String)(Object) b.toString();
				b = (Object) b.toString();
				b = (String) b.toString();
				if (true)
					break aaa;
			}
			if ("war".equals("peace"))
				continue;
			else if (1 == 0)
				break;

			int x = 6;
			x ++;
		} while (false);

		if (false)
			return;

		for (int i = 0; i < 3; i++) {
			for (int j = 0; i < 4; i++) {
				System.out.println(i + "*" + j + "=" + (i * j));
			}
		}
		a: for (int i = 0; i < 3; i++) {
			b: for (int j = 0; j < 10; j++) {
				c: for (int k = 0; k < 10; k++) {
					if (true)
						break c;
				}

				if ('c' == 'd')
					continue b;
			}

			if (null instanceof Object)
				break a;
		}
		a: for (int i = 0; i < 3; i++) {
			b: for (int j = 0; j < i; i++) {
				c: for (int k = 0; k < j; k++) {
					if (true)
						;
					else
						continue c;
				}
				if ("freedom".equals("slavery"))
					break b;
			}
			if ("ignorance" == "strength")
				continue a;
		}

		foo:
		;

	}
}
