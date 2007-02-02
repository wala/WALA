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

public interface IGrammar<T extends IProductionRule> extends Cloneable {
    public IVariable getStartSymbol();
    
    public Set<IVariable> getNonterminals();

    // public Set<IProductionRule> getRules();
    public Set<T> getRules();
    public Set<T> getRules(IVariable v);
    
    public void traverseRules(IRuleVisitor visitor);
    public void traverseSymbols(ISymbolVisitor visitor);
    public void traverse(IGrammarVisitor<T> visitor);
    
    public IGrammar copy(IGrammarCopier<T> copier);
    public Object clone();
}
