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

public class FunkySupers {
	int y;
	int funky(FunkySupers fs) {
		return 5;
	}
	
	public static void main(String args[]) {
		new SubFunkySupers().funky(new FunkySupers());
	}
}

class SubFunkySupers extends FunkySupers {
	
  @Override
  int funky(FunkySupers fs) {
		SubFunkySupers.super.funky(fs);
		SubFunkySupers.this.funky(fs);
		SubFunkySupers.this.y = 7;
		SubFunkySupers.super.y = 7;
		super.y = 7;
		super.funky(fs);
		return 6;
	}
}

//class EE { class X {} }
//class Y extends EE.X { Y(EE e) { e.super(); } }
// DOESNT WORK IN POLYGLOT!!!
