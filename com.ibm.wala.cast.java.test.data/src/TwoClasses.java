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
public class TwoClasses {
    private int value;
    private float fval= 3.14F;
    public TwoClasses(int x) {
	value = x;
    }
    public TwoClasses() {
	this(0);
    }
    public static void doStuff(int N) {
	int prod= 1;
	TwoClasses tc= new TwoClasses();
	tc.instanceMethod1();
	for(int j=0; j < N; j++)
	    prod *= j;
    }
    public static void main(String[] args) {
	int sum= 0;
	for(int i=0; i < 10; i++) {
	    sum += i;
	}
	TwoClasses.doStuff(sum);
    }
    public void instanceMethod1() {
	instanceMethod2();
    }
    public void instanceMethod2() {
	Bar b= Bar.create('a');
    }
}
class Bar {
    private final char fChar;

    private Bar(char c) {
	fChar= c;
    }
    public static Bar create(char c) {
	return new Bar(c);
    }
}
