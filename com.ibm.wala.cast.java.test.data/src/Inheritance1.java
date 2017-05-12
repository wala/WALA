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
public class Inheritance1 {
    public static void main(String[] args) {
	@SuppressWarnings("unused")
	Inheritance1 ih1= new Inheritance1();
	Base b1 = new Base();
	Base b2 = new Derived();

	b1.foo();
	b2.foo();
	b1.bar(3);
	b2.bar(5);
    }
}
class Base {
    public void foo() {
	@SuppressWarnings("unused")
	int i= 0;
    }
    public String bar(int x) {
	return Integer.toOctalString(x);
    }
}
class Derived extends Base {
    
    public void foo() {
	super.foo();
    }
    
    public String bar(int x) {
	return Integer.toHexString(x);
    }
}
