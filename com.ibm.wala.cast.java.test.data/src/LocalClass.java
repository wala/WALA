/*
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
public class LocalClass {
    public static void main(String[] args) {
	final Integer base = 6;

	class Foo {
	    int value;
	    public Foo(int v) { value= v; }
	    public int getValue() { return value; }
	    public int getValueBase() { return value - base; }
	}
	Foo f= new Foo(3);

	System.out.println(f.getValue());
	System.out.println(f.getValueBase());

	(new LocalClass()).method();
    }

    public void method() {
	final Integer base = 6;

	class Foo {
	    int value;
	    public Foo(int v) { value= v; }
	    public int getValue() { return value; }
	    public int getValueBase() { return value - base; }
	}
	Foo f= new Foo(3);

	System.out.println(f.getValue());
	System.out.println(f.getValueBase());
    }
}
