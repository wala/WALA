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
public class InnerClass {
    public static void main(String[] args) {
      (new InnerClass()).method();
    }

    public int fromInner(int v) {
      return v + 1;
    }

    public int fromInner2(int v) {
      return v + 3;
    }

    public void method() {
	WhatsIt w= new WhatsIt();
    }

    class WhatsThat {
      private int otherValue;

      WhatsThat() {
	otherValue = 3;
	fromInner2( otherValue );
      }
    }

    class WhatsIt {
	private int value;

	public WhatsIt() {
	  value= 0;
	  fromInner(value);
	  anotherMethod();
	}

	private NotAgain anotherMethod() {
	  return new NotAgain();
	}

	class NotAgain {
	  Object x;

	  public NotAgain() {
	    x = new WhatsThat();
	  }

	}
    }
}
