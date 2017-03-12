/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package p;

public class NonPrimaryTopLevel {
    public static void main(String[] args) {
	NonPrimaryTopLevel nptl= new NonPrimaryTopLevel();
	Foo f = new Foo();
    }
}

class Foo {
    
}
