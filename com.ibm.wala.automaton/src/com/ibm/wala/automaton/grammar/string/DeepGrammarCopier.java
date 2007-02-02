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
package com.ibm.wala.automaton.grammar.string;

import java.util.Collection;

import com.ibm.wala.automaton.string.ISymbol;

public class DeepGrammarCopier implements IGrammarCopier {
    private IRuleCopier ruleCopier;
    
    public DeepGrammarCopier(IRuleCopier ruleCopier) {
        this.ruleCopier = ruleCopier;
    }
    
    public IGrammar copy(IGrammar grammar) {
        return (IGrammar) grammar.clone();
    }

    public IProductionRule copy(IProductionRule rule) {
        return ruleCopier.copy(rule);
    }

    public Collection copyRules(Collection rules) {
        return ruleCopier.copyRules(rules);
    }

    public ISymbol copy(ISymbol symbol) {
        return ruleCopier.copy(symbol);
    }

    public Collection copySymbols(Collection symbols) {
        return ruleCopier.copySymbols(symbols);
    }

    public String copyName(String name) {
        return ruleCopier.copyName(name);
    }

    public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol) {
        return ruleCopier.copySymbolReference(parent, symbol);
    }

    public Collection copySymbolReferences(ISymbol parent, Collection symbols) {
        return ruleCopier.copySymbolReferences(parent, symbols);
    }
}
