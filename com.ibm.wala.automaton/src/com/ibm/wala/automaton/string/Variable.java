/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.automaton.string;

public class Variable extends Symbol implements ISymbol, IVariable {
    public Variable(String name){
        super(name);
    }
    
    public Variable(IVariable v) {
        this(v.getName());
    }

    public boolean matches(ISymbol s, IMatchContext ctx) {
        ctx.put(this, s);
        return true;
    }
    
    public String toString(){
        return "$" + getName();
    }
}
