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
public class StaticNesting {
    @SuppressWarnings("unused")
    public static void main(String[] args) {
	StaticNesting sn= new StaticNesting();
	WhatsIt w= new WhatsIt();
    }
    static class WhatsIt {
	private int value;
	public WhatsIt() {
	    value= 0;
	}
    }
}
