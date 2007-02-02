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

import java.util.Collection;

public class SimpleTransitionCopier extends AbstractTransitionCopier implements ITransitionCopier {
    public ITransition copy(ITransition transition) {
        return (ITransition) transition.clone();
    }

    public IState copy(IState state) {
        return state;
    }

    public Collection copyStates(Collection c) {
        return c;
    }

    public String copyName(String name) {
        return name;
    }
    
    public String copyStateName(String name) {
        return name;
    }

    public IState copyStateReference(IState parent, IState state) {
        return state;
    }

    public Collection copyStateReferences(IState parent, Collection c) {
        return c;
    }

    public ISymbol copy(ISymbol symbol) {
        return symbol;
    }

    public Collection copySymbols(Collection symbols) {
        return symbols;
    }

    public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol) {
        return symbol;
    }
    
    public Collection copySymbolReferences(ISymbol parent, Collection c) {
        return c;
    }

    static public SimpleTransitionCopier defaultCopier = new SimpleTransitionCopier();
}
