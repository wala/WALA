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
public class InheritedField {
    public static void main(String[] args) {
	InheritedField if1= new InheritedField();
	B b = new B();

	b.foo();
	b.bar();
    }
}
class A {
    protected int value;
}
class B extends A {
    public void foo() {
	value = 10;
    }
    public void bar() {
	this.value *= 2;
    }
}
