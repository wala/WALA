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

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.string.ISymbol;

public class SimpleGrammarCopier implements IGrammarCopier {
    public IGrammar copy(IGrammar grammar) {
        return (IGrammar) grammar.clone();
    }

    public IProductionRule copy(IProductionRule rule) {
        return rule;
    }

    public Collection copyRules(Collection rules) {
        return rules;
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

    static final public SimpleGrammarCopier defaultCopier = new SimpleGrammarCopier();
}
