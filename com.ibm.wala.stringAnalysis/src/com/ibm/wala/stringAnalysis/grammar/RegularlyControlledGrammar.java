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
package com.ibm.wala.stringAnalysis.grammar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.AbstractGrammar;
import com.ibm.wala.automaton.grammar.string.ContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.Grammars;
import com.ibm.wala.automaton.grammar.string.IGrammar;
import com.ibm.wala.automaton.grammar.string.IGrammarCopier;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleGrammar;
import com.ibm.wala.automaton.grammar.string.SimpleGrammarCopier;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.ISTSCopier;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.IStateTransitionSystem;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.SimpleSTSCopier;
import com.ibm.wala.stringAnalysis.util.SAUtil;

public class RegularlyControlledGrammar<T extends IProductionRule> extends AbstractGrammar<T> implements IRegularlyControlledGrammar<T> {
    private IAutomaton automaton;
    private Set<ISymbol> fails;
    private Map<ISymbol,T> ruleMap;
    
    private RegularlyControlledGrammar() {
        this.fails = new HashSet<ISymbol>();
        this.ruleMap = new HashMap<ISymbol,T>();
        setAutomaton(automaton);
    }
    
    public RegularlyControlledGrammar(IAutomaton automaton, Set<ISymbol> fails, Map<ISymbol,T> ruleMap) {
        this();
        this.fails.addAll(fails);
        this.ruleMap.putAll(ruleMap);
        setAutomaton(automaton);
    }
    
    public RegularlyControlledGrammar(IAutomaton automaton, ISymbol fails[], ISymbol ruleMapKeys[], T ruleMapVals[]) {
        this(automaton, SAUtil.set(fails), SAUtil.map(ruleMapKeys,ruleMapVals));
    }
    
    public RegularlyControlledGrammar(IAutomaton automaton) {
        this();
        setAutomaton(automaton);
    }
    
    public RegularlyControlledGrammar(IRegularlyControlledGrammar g) {
        this(g.getAutomaton(), g.getFails(), g.getRuleMap());
    }
    
    private void setAutomaton(IAutomaton automaton) {
        // TODO: normalize?
        this.automaton = automaton;
    }
    
    /* (non-Javadoc)
     * @see com.ibm.domo.sa.grammar.IRegularControlledGrammar#getAutomaton()
     */
    public IAutomaton getAutomaton() {
        return automaton;
    }
    
    /* (non-Javadoc)
     * @see com.ibm.domo.sa.grammar.IRegularControlledGrammar#getFails()
     */
    public Set<ISymbol> getFails() {
        return fails;
    }
    
    /* (non-Javadoc)
     * @see com.ibm.domo.sa.grammar.IRegularControlledGrammar#getRuleMap()
     */
    public Map<ISymbol,T> getRuleMap() {
        return ruleMap;
    }
    
    public IVariable getStartSymbol() {
        IState initState = automaton.getInitialState();
        Set<ITransition> transitions = automaton.getTransitions(initState);
        Set<IVariable> variables = new HashSet();
        for (Iterator i = transitions.iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            ISymbol isym = t.getInputSymbol();
            //Assertions._assert(isym!=null, "the automaton should be epsilon-free");
            if (isym != null) {
                IProductionRule rule = (IProductionRule) ruleMap.get(isym);
                IVariable v = rule.getLeft();
                variables.add(v);
            }
        }
        //Assertions._assert(variables.size()==1, "the automaton should have only an initial input symbol");
        if (variables.size()==1) {
            return (IVariable)(new ArrayList(variables)).get(0);
        }
        else {
            return null;
        }
    }
    
    public Set<T> getRules() {
        return new HashSet<T>(ruleMap.values());
    }
    
    public Set<T> getRules(IVariable v) {
        return Grammars.getRules(this, v);
    }
    
    /* (non-Javadoc)
     * @see com.ibm.domo.sa.grammar.IRegularControlledGrammar#assignRule(com.ibm.capa.util.automaton.string.IState, com.ibm.capa.util.grammar.string.IProductionRule)
     */
    public void assignRule(ISymbol symbol, T rule) {
        ruleMap.put(symbol, rule);
    }
    
    /* (non-Javadoc)
     * @see com.ibm.domo.sa.grammar.IRegularControlledGrammar#assignRules(java.util.Map)
     */
    public void assignRules(Map<ISymbol,T> ruleMap) {
        this.ruleMap.putAll(ruleMap);
    }
    
    /* (non-Javadoc)
     * @see com.ibm.domo.sa.grammar.IRegularControlledGrammar#assignRules(com.ibm.capa.util.automaton.string.IState[], com.ibm.capa.util.grammar.string.IProductionRule[])
     */
    public void assignRules(ISymbol symbols[], T rules[]) {
        assignRules(SAUtil.map(symbols, rules));
    }
    
    /* (non-Javadoc)
     * @see com.ibm.domo.sa.grammar.IRegularControlledGrammar#getRule(com.ibm.capa.util.automaton.string.IState)
     */
    public T getRule(ISymbol symbol) {
        return ruleMap.get(symbol);
    }
    
    /**
     * approximate the regular controlled grammar by a context-free grammar.  
     */
    public SimpleGrammar<IProductionRule> toSimple() {
        Set<IProductionRule> rules = new HashSet<IProductionRule>(ruleMap.values());
        IState initState = automaton.getInitialState();
        IVariable startVar = getStartSymbol();
        ContextFreeGrammar<IProductionRule> cfg = new ContextFreeGrammar<IProductionRule>(startVar, rules);
        return cfg;
    }
    
    static public interface ICopier extends IGrammarCopier, ISTSCopier {
        Map copyRuleMap(Map map);
    }
    
    static public class DeepCopier implements ICopier {
        private ISTSCopier sCopier;
        private IGrammarCopier gCopier;
        
        public DeepCopier() {
            this(SimpleGrammarCopier.defaultCopier, SimpleSTSCopier.defaultCopier);
        }
        
        public DeepCopier(IGrammarCopier gCopier, ISTSCopier sCopier) {
            this.sCopier = sCopier;
            this.gCopier = gCopier;
        }

        public Map copyRuleMap(Map map) {
            Map m = new HashMap();
            for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
                ISymbol s = (ISymbol) i.next();
                IProductionRule r = (IProductionRule) map.get(s);
                s = s.copy(gCopier);
                r = r.copy(gCopier);
                m.put(s, r);
            }
            return m;
        }

        public IGrammar copy(IGrammar grammar) {
            return grammar.copy(gCopier);
        }

        public IProductionRule copy(IProductionRule rule) {
            return rule.copy(gCopier);
        }

        public Collection copyRules(Collection rules) {
            return gCopier.copyRules(rules);
        }

        public ISymbol copy(ISymbol symbol) {
            return symbol.copy(gCopier);
        }

        public Collection copySymbols(Collection symbols) {
            return gCopier.copySymbols(symbols);
        }

        public String copyName(String name) {
            return gCopier.copyName(name);
        }
        
        public String copyStateName(String name) {
            return sCopier.copyStateName(name);
        }

        public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol) {
            return gCopier.copySymbolReference(parent, symbol);
        }

        public Collection copySymbolReferences(ISymbol parent, Collection symbols) {
            return gCopier.copySymbolReferences(parent, symbols);
        }

        public IStateTransitionSystem copy(IStateTransitionSystem sts) {
            return sts.copy(sCopier);
        }

        public ITransition copy(ITransition transition) {
            return transition.copy(sCopier);
        }

        public Collection copyTransitions(Collection transitions) {
            return sCopier.copyTransitions(transitions);
        }

        public IState copy(IState state) {
            return state.copy(sCopier);
        }

        public Collection copyStates(Collection states) {
            return sCopier.copyStates(states);
        }
        
        public IState copyStateReference(IState parent, IState state) {
            return sCopier.copyStateReference(parent, state);
        }
        
        public Collection copyStateReferences(IState parent, Collection c) {
            return sCopier.copyStateReferences(parent, c);
        }
    }
    
    public IGrammar<T> copy(final IGrammarCopier<T> copier) {
        IGrammar<T> g = copier.copy(this);
        if (!getClass().isInstance(g)) {
            return g;
        }
        ICopier thisCopier = null;
        if (copier instanceof ICopier) {
            thisCopier = (ICopier) copier;
        }
        else {
            thisCopier = new DeepCopier(copier, SimpleSTSCopier.defaultCopier);
        }
        RegularlyControlledGrammar<T> rg = (RegularlyControlledGrammar<T>) g;
        rg.ruleMap = new HashMap(thisCopier.copyRuleMap(ruleMap));
        rg.automaton = (IAutomaton) automaton.copy(thisCopier);
        rg.fails = new HashSet(thisCopier.copySymbols(fails));
        return g;
    }
    
    public int hashCode() {
        return automaton.hashCode() + fails.hashCode() + ruleMap.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!getClass().equals(obj.getClass())) return false;
        IRegularlyControlledGrammar g = (IRegularlyControlledGrammar) obj;
        return automaton.equals(g.getAutomaton())
            && fails.equals(g.getFails())
            && ruleMap.equals(g.getRuleMap());
    }
    
    public String toString(Set history) {
        if (history.contains(this)) {
            return "...";
        }
        history.add(this);
        
        StringBuffer rulesStr = new StringBuffer();
        List rules = new ArrayList();
        for (Iterator i = ruleMap.keySet().iterator(); i.hasNext(); ) {
            ISymbol s = (ISymbol) i.next();
            IProductionRule r = (IProductionRule) ruleMap.get(s);
            rules.add(new Object[]{s,r});
        }
        Collections.sort(rules, new Comparator(){
            public int compare(Object o1, Object o2) {
                Object pair1[] = (Object[]) o1;
                Object pair2[] = (Object[]) o2;
                IProductionRule r1 = (IProductionRule) pair1[1];
                IProductionRule r2 = (IProductionRule) pair2[1];
                return r1.getLeft().getName().compareTo(r2.getLeft().getName());
            }
        });
        for (Iterator i = rules.iterator(); i.hasNext(); ) {
            Object pair[] = (Object[])i.next();
            ISymbol s = (ISymbol) pair[0];
            IProductionRule r = (IProductionRule) pair[1];
            if (r instanceof GInvocationRule) {
                GInvocationRule grule = (GInvocationRule) r;
                rulesStr.append(s.toString() + ": " + grule.toString(history));
            }
            else {
                rulesStr.append(s.toString() + ": " + r.toString());
            }
            if (i.hasNext()) {
                rulesStr.append(", ");
            }
        }
        return "{rules:" + "{" + rulesStr + "}"
            + ", automaton:" + automaton.toString()
            + ", fails:" + fails.toString();
    }
    
    public String toString() {
        return toString(new HashSet());
    }
}
