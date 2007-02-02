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

public abstract class AbstractGrammar<T extends IProductionRule> implements IGrammar<T> {
    public void traverseSymbols(final ISymbolVisitor visitor) {
        if (getStartSymbol() != null) {
          getStartSymbol().traverse(visitor);
        }
        traverseRules(new IRuleVisitor(){
            public void onVisit(IProductionRule rule) {
                rule.traverseSymbols(visitor);
            }
        });
    }
    
    public void traverseRules(IRuleVisitor visitor) {
        for (Iterator i = getRules().iterator(); i.hasNext(); ) {
            IProductionRule rule = (IProductionRule) i.next();
            rule.traverse(visitor);
        }
    }
    
    public void traverse(IGrammarVisitor<T> visitor) {
        visitor.onVisit(this);
        traverseSymbols(visitor);
        traverseRules(visitor);
    }

    public Set<IVariable> getNonterminals() {
        // Set<IProductionRule> l = new ArrayList();
        Set<IVariable> l = new HashSet<IVariable>();
        l.add(getStartSymbol());
        for (Iterator i = getRules().iterator(); i.hasNext(); ) {
            IProductionRule rule = (IProductionRule) i.next();
            l.add(rule.getLeft());
            for (Iterator j = rule.getRight().iterator(); j.hasNext(); ) {
                ISymbol s = (ISymbol) j.next();
                if (s instanceof IVariable) {
                    l.add((IVariable)s);
                }
            }
        }
        return l;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw(new RuntimeException(e));
        }
    }
}
