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
public class WhileTest1 {
    public static void main(String[] args) {
	@SuppressWarnings("unused")
	WhileTest1 wt1= new WhileTest1();
	int x= 235834;
	boolean stop= false;

	while (!stop) {
	    x += 3;
	    x ^= 0xAAAA5555;
	    stop= (x & 0x1248) != 0;
	}

	while (!stop) {
	    x += 3;
	    if (x < 7) continue;
	    x ^= 0xAAAA5555;
	    stop= (x & 0x1248) != 0;
	}
    }
}
