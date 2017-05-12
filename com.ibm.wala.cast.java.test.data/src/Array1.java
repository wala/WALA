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
public class Array1 {
    public static void main(String[] args) {
	Array1 f= new Array1();
	f.foo();
    }
    public void foo() {
	int[] ary = new int[5];

	for(int i= 0; i < ary.length; i++) {
	    ary[i]= i;
	}

	@SuppressWarnings("unused")
	int sum = 0;

	for(int j= 0; j < ary.length; j++) {
	    sum += ary[j];
	}
    }
}
