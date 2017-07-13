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
package reflection;


public class Reflect6 {

	public static void main(String[] args) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
		Class<?> c = Class.forName("reflection.Reflect6$A");
		A h = (A) c.newInstance();
		System.out.println(h.toString());
	}
	
	public static class A {
		private A(int i) {
		}
		@Override
    public String toString() {
			return "Instance of A";
		}
	}
}
