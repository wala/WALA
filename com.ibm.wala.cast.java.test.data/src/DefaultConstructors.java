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

public class DefaultConstructors {
	public static void main(String args[]) {
		E e = new E();
//		System.out.println(e.x);
//		System.out.println(e.y);
		e = new E(7,8);
//		System.out.println(e.x);
//		System.out.println(e.y);

		Object x[] = new Object[4];
		x.clone();
		x.equals(new Object());
		x.toString();
	}
}

class dcA {
	int x;
	dcA() {
		x = 5;
	}
	dcA(int a) {
		x = a;
	}
}

class dcB extends dcA {
	dcB() {
		super();
	}
}

class C extends dcA {
	C() {
		// implicit call to super(). we want to see if it's the same as above
	}
}

class D extends dcA {
	// implicit constructor, should be same as above
}

class E extends dcA {
	int y;
	E() {
		// no implicit call
		this(6);
	}
	
	E(int z) {
		// implicit call to A()
		this.y = z;
	}
	
	E(int a, int b) {
		// no implicit call
		super(a);
		y = b;
	}
}
