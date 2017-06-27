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

interface ISimpleCalls {
	public void helloWorld();
}
public class SimpleCalls implements ISimpleCalls {
	
  @Override
  public void helloWorld() {
		System.out.println("hello world!");
	}
	public int anotherCall() {
		this.helloWorld();
		((this)).helloWorld();
		System.out.println("another call");
		return 5;
	}
	public static void main (String args[]) {
		SimpleCalls sc = new SimpleCalls();
		ISimpleCalls isc = sc;
		isc.helloWorld();
		int y = sc.anotherCall();
		y = y + y;
	}
}
