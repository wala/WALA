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

public class Exception1 {
    public static void main(String[] args) {
	@SuppressWarnings("unused")
	Exception1 e1= new Exception1();
	try {
	    FooEx1 f = new FooEx1();

	    f.bar();
	} catch(BadLanguageExceptionEx1 e) {
	    e.printStackTrace();
	}
	try {
	    FooEx2 f = new FooEx2();

	    f.bar();
	} catch(Throwable e) {
	    e.printStackTrace();
	}
    }
}

class FooEx1 {
    public void bar() throws BadLanguageExceptionEx1 {
	throw new BadLanguageExceptionEx1();
    }
}

class FooEx2 {
    public void bar() {
	throw new NullPointerException();
    }
}

class BadLanguageExceptionEx1 extends Exception {
    public BadLanguageExceptionEx1() {
	super("Try using a real language, like Perl");
    }
}
