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

class Super {
	String s;

	Super(String x) {
		super();
		s = x;
	}
}

public class ConstructorsAndInitializers extends Super {
	static int iX;
	static int sX;

	Object x;

	static {
		sX = 11;
	}

	{
		iX = 55 + 33;
		sX = 6;
	}

	ConstructorsAndInitializers() {
		this("hello");
	}

	{
		int y = 5;
		iX = y + 9;

		x = new Super("yo wassup") {

			{
				s += " i say wassup!";
				iX = 55 + 12;
			}

      @Override
      public String toString() {
				return s + " -- from an anon class in " + iX + "," + sX + ": " + ConstructorsAndInitializers.this.s;
			}
			
			{
				s += " i say i say";
			}
		};
	}

	protected ConstructorsAndInitializers(String x) {
		super("yo");
		iX = 99;
	}

	static {
		sX = 22;
	}

	public static void main(String args[]) {
		System.out.println(new ConstructorsAndInitializers().x.toString()); // yo wassup i say wassup! i say i say -- from an anon class in 99,6: yo

		class T{
			
		}
		T t = new T();
	}
}

