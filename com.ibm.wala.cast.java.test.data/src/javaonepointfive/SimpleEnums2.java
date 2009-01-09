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

public class SimpleEnums2 {
	public enum Direction {
	    NORTH,
	    EAST,
	    SOUTH,
	    WEST;
	    public static void hello() {
	    	System.out.println(NORTH);
	    }
	    
	    public static Direction[] myValues() {
	    	return new Direction[] { NORTH, EAST, SOUTH, WEST };
	    }
	}

	public static void main(String args[]) {
		(new SimpleEnums2()).doit();
	}
	
	private void doit() {
		System.out.println("never eat shredded wheat");
		System.out.println(Direction.NORTH);
		System.out.println(Direction.EAST);
		System.out.println(Direction.SOUTH);
		System.out.println(Direction.WEST);
		
		Direction abc = Enum.valueOf(Direction.class, "NORTH");
		Direction efg = Direction.valueOf("NORTH");
		System.out.println(abc);
		System.out.println(efg);
		
		Direction x = Direction.values()[0];
		System.out.println(x);
		Direction y = Direction.myValues()[0];
		System.out.println(y);
	}
	
//	public static class Foo {
//		public static final Foo foo = new Foo("NORTH",1);
//		public Foo(String string, int i) {
//		}
//	}
//	public static void main(String args[]) {
//		System.out.println(Foo.foo);
//	}
	
}
