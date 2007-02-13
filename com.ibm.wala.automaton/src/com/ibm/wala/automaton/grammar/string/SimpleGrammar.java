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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.string.IVariable;

public class SimpleGrammar<T extends IProductionRule> extends AbstractGrammar<T> implements ISimplify {
    // private Set<IProductionRule> rules;
    private Set<T> rules;
    private IVariable startSymbol;
    
    public SimpleGrammar(){
        rules = new HashSet<T>();
        startSymbol = null;
    }
    
    public SimpleGrammar(IVariable startSymbol, Collection<T> rules){
        this();
        setStartSymbol(startSymbol); 
        addRules(rules);
    }

    public SimpleGrammar(IVariable startSymbol, T rules[]){
        this();
        List<T> l = new ArrayList<T>();
        for (int i = 0; i < rules.length; i++ ) {
            l.add(rules[i]);
        }
        setStartSymbol(startSymbol);
        addRules(l);
    }
    
    public SimpleGrammar(SimpleGrammar<T> g) {
        this(g.getStartSymbol(), g.getRules());
    }

    public IVariable getStartSymbol() {
        return startSymbol;
    }
    
    public void setStartSymbol(IVariable startSymbol){
        this.startSymbol = startSymbol;
    }

    public Set<T> getRules() {
        return rules;
    }
    
    public Set<T> getRules(IVariable v) {
        return Grammars.getRules(this, v);
    }
        
    public void addRule(T rule) {
        rules.add(rule);
    }

    public void addRules(Collection<T> rules) {
        addRules(rules.iterator());
    }

    public void addRules(Iterator<T> rules) {
        while (rules.hasNext()) {
            addRule(rules.next());
        }
    }
        
    public IGrammar copy(IGrammarCopier<T> copier) {
        IGrammar g = copier.copy(this);
        if (g instanceof SimpleGrammar) {
            SimpleGrammar<IProductionRule> cfg = (SimpleGrammar<IProductionRule>) g;
            cfg.rules = new HashSet<IProductionRule>(copier.copyRules(cfg.rules));
            cfg.startSymbol = (IVariable) copier.copy(cfg.startSymbol);
        }
        return g;
    }
    
    public int hashCode() {
        return getClass().hashCode();
        /*
        return rules.hashCode() + startSymbol.hashCode();
        */
    }
    
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!getClass().equals(obj.getClass())) return false;
        
        SimpleGrammar cfg = (SimpleGrammar) obj;
        /*
         * TODO: why do we need this code?
         */
        HashSet<T> s = new HashSet<T>(rules);
        rules.clear();
        rules.addAll(s);
        return startSymbol.equals(cfg.getStartSymbol())
            && rules.equals(cfg.getRules());
    }
    
    public String toString() {
        StringBuffer rulesStr = new StringBuffer();
        List rules = new ArrayList();
        for (Iterator i = AUtil.sort(getRules()).iterator(); i.hasNext(); ){
            IProductionRule rule = (IProductionRule) i.next();
            rules.add(rule);
        }
        Collections.sort(rules, new Comparator(){
            public int compare(Object o1, Object o2) {
                IProductionRule r1 = (IProductionRule) o1;
                IProductionRule r2 = (IProductionRule) o2;
                return r1.getLeft().getName().compareTo(r2.getLeft().getName());
            }
        });
        for (Iterator i = rules.iterator(); i.hasNext(); ) {
            IProductionRule rule = (IProductionRule) i.next();
            rulesStr.append(rule.toString());
            if (i.hasNext()) {
                rulesStr.append("; ");
            }
        }
        return "{start:" + getStartSymbol() + ", rules:{" + rulesStr + "}}";
    }
    
    public SimpleGrammar<T> toSimple() {
      return this;
    }
}
