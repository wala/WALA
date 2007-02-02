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

public class DeepSTSCopier implements ISTSCopier {
    private ITransitionCopier tCopier;
    
    public DeepSTSCopier(ITransitionCopier tCopier) {
        this.tCopier = tCopier;
    }

    public IStateTransitionSystem copy(IStateTransitionSystem sts) {
        return (IStateTransitionSystem) sts.clone();
    }

    public ITransition copy(ITransition transition) {
        return transition.copy(tCopier);
    }

    public Collection copyTransitions(Collection transitions) {
        return tCopier.copyTransitions(transitions);
    }

    public IState copy(IState state) {
        return state.copy(tCopier);
    }

    public Collection copyStates(Collection c) {
        return tCopier.copyStates(c);
    }

    public String copyStateName(String name) {
        return tCopier.copyStateName(name);
    }

    public IState copyStateReference(IState parent, IState state) {
        return tCopier.copyStateReference(parent, state);
    }

    public Collection copyStateReferences(IState parent, Collection c) {
        return tCopier.copyStateReferences(parent, c);
    }

    public ISymbol copy(ISymbol symbol) {
        return symbol.copy(tCopier);
    }

    public Collection copySymbols(Collection symbols) {
        return tCopier.copySymbols(symbols);
    }

    public String copyName(String name) {
        return tCopier.copyName(name);
    }

    public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol) {
        return tCopier.copySymbolReference(parent, symbol);
    }

    public Collection copySymbolReferences(ISymbol parent, Collection symbols) {
        return tCopier.copySymbolReferences(parent, symbols);
    }

}
