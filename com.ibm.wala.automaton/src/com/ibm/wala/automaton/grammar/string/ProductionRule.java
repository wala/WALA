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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ISymbolVisitor;
import com.ibm.wala.automaton.string.IVariable;

public class ProductionRule implements IProductionRule {
    private IVariable left;
    private List sequence;
    
    public ProductionRule(IVariable left, List sequence) {
        init(left, sequence);
    }
    
    public ProductionRule(IVariable left, ISymbol sequence[]) {
        List l = new ArrayList();
        for (int i = 0; i < sequence.length; i++) {
            l.add(sequence[i]);
        }
        init(left, l);
    }
    
    public ProductionRule(IVariable left, ISymbol right) {
        this(left, new ISymbol[]{right});
    }
    
    public ProductionRule(IProductionRule rule){
        init(rule.getLeft(), rule.getRight());
    }
    
    private void init(IVariable left, List sequence){
        this.left = left;
        this.sequence = new ArrayList();
        addRight(sequence);
    }
    
    private void addRight(List sequence) {
        for (Iterator i = sequence.iterator(); i.hasNext(); ) {
            addRight((ISymbol)i.next());
        }
    }
    
    private void addRight(ISymbol symbol) {
        this.sequence.add(symbol);
    }
    
    public IVariable getLeft() {
        return left;
    }

    public void setLeft(IVariable left) {
        this.left = left;
    }

    public List getRight() {
        return sequence;
    }
    
    public ISymbol getRight(int index) {
        return (ISymbol) sequence.get(index);
    }
    
    
    public void traverseSymbols(ISymbolVisitor visitor) {
        left.traverse(visitor);
        for (Iterator i = sequence.iterator(); i.hasNext(); ) {
            ISymbol s = (ISymbol) i.next();
            s.traverse(visitor);
        }
    }
    
    public void traverse(IRuleVisitor visitor) {
        visitor.onVisit(this);
    }
    
    public boolean isEpsilonRule(){
        return sequence.isEmpty();
    }
    
    public int hashCode(){
        return 0;
        /*
        return left.hashCode() + sequence.hashCode();
        */
    }
    
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!getClass().equals(obj.getClass())) return false;
        ProductionRule rule = (ProductionRule) obj;
        return left.equals(rule.left)
            && sequence.equals(rule.sequence);
    }
    
    public String toString(){
        StringBuffer buff = new StringBuffer();
        buff.append("[");
        for (Iterator i = getRight().iterator(); i.hasNext(); ) {
            ISymbol symbol = (ISymbol) i.next();
            buff.append(symbol);
            if (i.hasNext()) {
                buff.append(", ");
            }
        }
        buff.append("]");
        return left.toString() + " -> " + buff.toString();
    }
    
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw(new RuntimeException(e));
        }
    }
    
    public IProductionRule copy(IRuleCopier copier) {
        IProductionRule rule = copier.copy(this);
        if (rule instanceof ProductionRule) {
            ProductionRule prule = (ProductionRule) rule;
            prule.left = (IVariable) copier.copy(prule.left);
            prule.sequence = new ArrayList(copier.copySymbols(prule.sequence));
        }
        return rule;
    }
}
