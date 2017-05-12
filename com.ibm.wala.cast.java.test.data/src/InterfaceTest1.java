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
public class InterfaceTest1 {
    public static void main(String[] args) {
	@SuppressWarnings("unused")
	InterfaceTest1 it= new InterfaceTest1();
	IFoo foo = new FooIT1('a');
	@SuppressWarnings("unused")
	char ch2 = foo.getValue();
    }
}

interface IFoo {
    char getValue();
}

class FooIT1 implements IFoo {
    private char fValue;
    public FooIT1(char ch) {
	fValue= ch;
    }

    public char getValue() {
	return fValue;
    }
}
