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

import java.util.*;

abstract public class AbstractSymbolCopier implements ISymbolCopier {

    public ISymbol copy(ISymbol symbol) {
        if (symbol == null) {
            return null;
        }
        else {
            return (ISymbol) symbol.clone();
        }
    }
    
    public Collection copySymbols(Collection symbols) {
        Collection c;
        try {
            c = (Collection) symbols.getClass().newInstance();
            for (Iterator i = symbols.iterator(); i.hasNext(); ) {
                ISymbol s = (ISymbol) i.next();
                c.add(s.copy(this));
            }
            return c;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw(new AssertionError("should not reach this code."));
    }
    
    abstract public String copyName(String name);

    abstract public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol);
    
    public Collection copySymbolReferences(ISymbol parent, Collection symbols) {
        Collection c;
        try {
            c = (Collection) symbols.getClass().newInstance();
            for (Iterator i = symbols.iterator(); i.hasNext(); ) {
                ISymbol s = (ISymbol) i.next();
                c.add(copySymbolReference(parent, s));
            }
            return c;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw(new AssertionError("should not reach this code."));
    }

}
