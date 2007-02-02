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

public class SimpleSTSCopier implements ISTSCopier {

    public IStateTransitionSystem copy(IStateTransitionSystem sts) {
        return (IStateTransitionSystem) sts.clone();
    }

    public ITransition copy(ITransition transition) {
        return transition;
    }

    public Collection copyTransitions(Collection transitions) {
        return transitions;
    }

    public IState copy(IState state) {
        return state;
    }

    public Collection copyStates(Collection c) {
        return c;
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

    public String copyName(String name) {
        return name;
    }

    public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol) {
        return symbol;
    }

    public Collection copySymbolReferences(ISymbol parent, Collection symbols) {
        return symbols;
    }

    static final public SimpleSTSCopier defaultCopier = new SimpleSTSCopier();
}
