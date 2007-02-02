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

import java.util.*;

import com.ibm.wala.automaton.string.*;

public class DeepRuleCopier extends AbstractRuleCopier {
    private ISymbolCopier symbolCopier;

    public DeepRuleCopier(ISymbolCopier symbolCopier) {
        this.symbolCopier = symbolCopier;
    }

    public IProductionRule copy(IProductionRule rule) {
        return (IProductionRule) rule.clone();
    }
    
    public ISymbol copy(ISymbol symbol) {
        return symbolCopier.copy(symbol);
    }

    public Collection copySymbols(Collection symbols) {
        return symbolCopier.copySymbols(symbols);
    }

    public String copyName(String name) {
        return symbolCopier.copyName(name);
    }

    public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol) {
        return symbolCopier.copySymbolReference(parent, symbol);
    }

    public Collection copySymbolReferences(ISymbol parent, Collection symbols) {
        return symbolCopier.copySymbolReferences(parent, symbols);
    }
}
