/******************************************************************************
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
public class InnerClassSuper {
	int x = 5;
	class SuperOuter {
		public void test() {
			System.out.println(x);
		}
	}
	public static void main(String args[]) {
		new Sub().new SubInner();
	}
}
class Sub extends InnerClassSuper {
	class SubInner {
		public SubInner() {
			InnerClassSuper.SuperOuter so = new InnerClassSuper.SuperOuter();
			so.test();
		}
	}
}
