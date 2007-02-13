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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.ContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.ISimplify;
import com.ibm.wala.automaton.grammar.string.SimpleGrammar;
import com.ibm.wala.automaton.string.Automaton;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ISymbolVisitor;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Symbol;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.stringAnalysis.util.SAUtil;

public class GR extends RegularlyControlledGrammar<GRule> {
    private IR ir;
    private Set<ISymbol> returnSymbols;
    private List<IVariable> parameterVariables;
    
    public void traverseSymbols(ISymbolVisitor visitor) {
        super.traverseSymbols(visitor);
        for (Iterator i = returnSymbols.iterator(); i.hasNext(); ) {
            ISymbol s = (ISymbol) i.next();
            s.traverse(visitor);
        }
        for (Iterator i = parameterVariables.iterator(); i.hasNext(); ) {
            ISymbol s = (ISymbol) i.next();
            s.traverse(visitor);
        }
    }
    
    private void setSpecialVariables(List<IVariable> parameters, Set<ISymbol> returns) {
        this.returnSymbols = new HashSet<ISymbol>();
        this.parameterVariables = new ArrayList<IVariable>();
        if (returns != null) {
            this.returnSymbols.addAll(returns);
        }
        if (parameters != null) {
            this.parameterVariables.addAll(parameters);
        }
    }
    
    public GR(IR ir, List<IVariable> parameters, Set<ISymbol> returns, IAutomaton automaton, Set<ISymbol> fails, Map<ISymbol,GRule> ruleMap) {
        super(automaton, fails, ruleMap);
        this.ir = ir;
        setSpecialVariables(parameters, returns);
    }
    
    public GR(IR ir, List<IVariable> parameters, Set<ISymbol> returns, IAutomaton automaton, ISymbol fails[], ISymbol inputs[], GRule rules[]) {
        super(automaton, fails, inputs, rules);
        this.ir = ir;
        setSpecialVariables(parameters, returns);
    }
    
    static public GR createGR(IR ir, Set<GRule> rules) {
      Map<ISymbol,GRule> ruleMap = new HashMap<ISymbol,GRule>();
      IAutomaton automaton = createAutomaton(rules, ruleMap);
      Set<ISymbol> fails = new HashSet<ISymbol>(SAUtil.collect(automaton.getTransitions(), new SAUtil.IElementMapper(){
        public Object map(Object obj) {
          ITransition t = (ITransition) obj;
          return t.getInputSymbol();
        }
      }));
      return new GR(ir, new ArrayList(), new HashSet(), automaton, fails, ruleMap);
    }
    
    static private IAutomaton createAutomaton(Set<GRule> rules, Map<ISymbol,GRule> ruleMap) {
      IState initState = new State("s0");
      IState finalState = new State("s1");
      Set<IState> finalStates = new HashSet<IState>();
      finalStates.add(finalState);
      Set<ITransition> transitions = new HashSet<ITransition>();
      int i = 0;
      for(GRule r : rules) {
        String s = "i" + i;
        ISymbol input = new Symbol(s);
        ruleMap.put(input, r);
        ITransition t = new Transition(initState, finalState, input);
        transitions.add(t);
        i++;
      }
      IAutomaton a = new Automaton(initState, finalStates, transitions);
      return a;
    }
    
    public GR(GR gr) {
        super(gr);
        this.ir = gr.getIR();
        setSpecialVariables(gr.getParameterVariables(), gr.getReturnSymbols());
    }
    
    public IR getIR() {
        return ir;
    }
    
    public SimpleGrammar<IProductionRule> toSimple() {
        return toCFG(new HashSet());
    }
    
    private SimpleGrammar<IProductionRule> toCFG(Set history) {
        SimpleGrammar<IProductionRule> cfg = super.toSimple();
        Set<IProductionRule> rules = new HashSet<IProductionRule>();
        for (IProductionRule rule : cfg.getRules()) {
            if (rule instanceof GInvocationRule) {
                GInvocationRule crule = (GInvocationRule) rule;
                rules.addAll(crule.getAliasRules());
                for (Iterator ig = crule.getGrammars().iterator(); ig.hasNext(); ) {
                    ISimplify g = (ISimplify) ig.next();
                    if (!history.contains(g)) {
                        history.add(g);
                        if (g instanceof GR) {
                            rules.addAll(((GR)g).toCFG(history).getRules());
                        }
                        else {
                            rules.addAll(g.toSimple().getRules());
                        }
                    }
                }
            }
            else {
                rules.add(rule);
            }
        }
        return new ContextFreeGrammar(cfg.getStartSymbol(), rules);
    }
    
    public Set<ISymbol> getReturnSymbols(){
        return this.returnSymbols;
    }
    
    public List<IVariable> getParameterVariables() {
        return this.parameterVariables;
    }
}
