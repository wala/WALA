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

public class WelcomeInitializers {
	int x;
	int y = 6;
	static int sX;
	static int sY = 6;
	
	{
		x = 7 / 7;
	}
	
	static { 
		sX = 9 / 3;
	}
	
	public WelcomeInitializers() {
		
	}
	public void hey() {}
	
	public static void main(String args[]) {
		new WelcomeInitializers().hey();
	}
}
