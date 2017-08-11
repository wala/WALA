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
public class AnonymousClass {
    private interface Foo {
      public int getValue();
      public int getValueBase();
    }

    public static void main(String[] args) {
	final Integer base = new Integer(6);

	Foo f= new Foo() {
	    int value = 3;
	    
      @Override
      public int getValue() { return value; }
	    
      @Override
      public int getValueBase() { return value - base.intValue(); }
	};

	System.out.println(f.getValue());
	System.out.println(f.getValueBase());

	(new AnonymousClass()).method();
    }

    public void method() {
	final Integer base = new Integer(7);

	abstract class FooImpl implements Foo {
	    int y;

	    
      @Override
      public abstract int getValue();

	    FooImpl(int _y) {
	      y = _y;
	    }

	    
      @Override
      public int getValueBase() { 
	      return y + getValue() - base.intValue(); 
	    }
	}

	Foo f= new FooImpl(-4) {
	  
    @Override
    public int getValue() { return 7; }
	};

	System.out.println(f.getValue());
	System.out.println(f.getValueBase());
    }
}
