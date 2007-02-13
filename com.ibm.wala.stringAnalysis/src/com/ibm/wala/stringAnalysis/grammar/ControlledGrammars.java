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

import com.ibm.wala.automaton.grammar.string.DeepGrammarCopier;
import com.ibm.wala.automaton.grammar.string.DeepRuleCopier;
import com.ibm.wala.automaton.grammar.string.Grammars;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleGrammarCopier;
import com.ibm.wala.automaton.string.Automatons;
import com.ibm.wala.automaton.string.DeepSymbolCopier;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.IStateTransitionSystem;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.IVariableFactory;
import com.ibm.wala.automaton.string.SimpleSTSCopier;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;

public class ControlledGrammars extends Grammars {
  static public IRegularlyControlledGrammar useUniqueStates(IRegularlyControlledGrammar g1, IRegularlyControlledGrammar g2, Map m) {
    return useUniqueStates(g1, Automatons.collectStateNames(g2.getAutomaton()), m);
  }

  static public IRegularlyControlledGrammar useUniqueStates(IRegularlyControlledGrammar g, Set baseNames, Map m) {
    final IAutomaton automaton = Automatons.useUniqueStates(g.getAutomaton(), baseNames, m);
    g = (IRegularlyControlledGrammar) g.copy(
      new RegularlyControlledGrammar.DeepCopier(){
        public IStateTransitionSystem copy(IStateTransitionSystem sts) {
          return automaton;
        }
      });
    return g;
  }

  static public IRegularlyControlledGrammar useUniqueInputSymbols(IRegularlyControlledGrammar g1, IRegularlyControlledGrammar g2, Map m) {
    return useUniqueInputSymbols(g1, Automatons.collectInputSymbolNames(g2.getAutomaton()), m);
  }

  static public IRegularlyControlledGrammar useUniqueInputSymbols(IRegularlyControlledGrammar g, Set baseNames, Map m) {
    final IAutomaton automaton = Automatons.useUniqueInputSymbols(g.getAutomaton(), baseNames, m);
    final Map ruleMap = new HashMap();
    for (Iterator i = g.getRuleMap().keySet().iterator(); i.hasNext(); ) {
      ISymbol symbol = (ISymbol) i.next();
      IProductionRule rule = g.getRule(symbol);
      ISymbol newSymbol = (ISymbol) m.get(symbol);
      ruleMap.put(newSymbol, rule);
    }
    final Set fails = new HashSet();
    for (Iterator i = g.getFails().iterator(); i.hasNext(); ) {
      ISymbol symbol = (ISymbol) i.next();
      ISymbol newSymbol = (ISymbol) m.get(symbol);
      fails.add(newSymbol);
    }

    g = (IRegularlyControlledGrammar) g.copy(
      new RegularlyControlledGrammar.DeepCopier(){
        public IStateTransitionSystem copy(IStateTransitionSystem sts) {
          return automaton;
        }
      });
    g.getRuleMap().clear();
    g.getRuleMap().putAll(ruleMap);
    g.getFails().clear();
    g.getFails().addAll(fails);
    return g;
  }

  /**
   * TODO: reuse useUniqueVariables(RegularlyControlledGrammar,Set,Map)
   * replace all the variables
   * @param g
   * @param baseNames
   * @param m
   * @return
   */
  static public GR useUniqueVariables(GR g, IVariableFactory varFactory, final Map m) {
    Set variables = g.getNonterminals();
    for (Iterator i = variables.iterator(); i.hasNext(); ) {
      IVariable v = (IVariable) i.next();
      IVariable newVar = varFactory.createVariable(variablePrefix);
      m.put(v, newVar);
    }

    final Map ruleMap = new HashMap(g.getRuleMap());
    for (Iterator i = ruleMap.keySet().iterator(); i.hasNext(); ) {
      ISymbol isym = (ISymbol) i.next();
      GRule rule = (GRule) ruleMap.get(isym);
      IR ir = rule.getIR();
      SSAInstruction instruction = rule.getSSAInstruction();
      IVariable left = (IVariable) m.get(rule.getLeft());
      List right = new ArrayList();
      for (Iterator j = rule.getRight().iterator(); j.hasNext(); ) {
        ISymbol s = (ISymbol) j.next();
        if (s instanceof IVariable && !(s instanceof LexicalVariable)) {
          right.add(m.get(s));
        }
        else {
          right.add(s);
        }
      }
      GRule newRule = null;
      if (rule instanceof GInvocationRule) {
        GInvocationRule crule = (GInvocationRule) rule;
        GInvocationRule newCRule = new GInvocationRule(crule.getGrammars(), ir, instruction, left, right);
        newCRule.getAliasRules().addAll(crule.getAliasRules());
        newRule = newCRule;
      }
      else {
        newRule = new GRule(ir, instruction, left, right);
      }
      ruleMap.put(isym, newRule);
    }
    List parameters = new ArrayList();
    for (Iterator i = g.getParameterVariables().iterator(); i.hasNext(); ) {
      IVariable v = (IVariable) i.next();
      parameters.add(m.get(v));
    }
    Set returns = new HashSet();
    for (Iterator i = g.getReturnSymbols().iterator(); i.hasNext(); ) {
      IVariable v = (IVariable) i.next();
      returns.add(m.get(v));
    }
    return new GR(g.getIR(), parameters, returns, g.getAutomaton(), g.getFails(), ruleMap);
  }

  /**
   * TODO: reuse useUniqueVariables(RegularlyControlledGrammar,Set,Map)
   * replace all the variables
   * @param g
   * @param baseNames
   * @param m
   * @return
   */
  static public GR appendSuffixToVariables(GR g, String suffix) {
    final Map ruleMap = new HashMap(g.getRuleMap());
    for (Iterator i = ruleMap.keySet().iterator(); i.hasNext(); ) {
      ISymbol isym = (ISymbol) i.next();
      GRule rule = (GRule) ruleMap.get(isym);
      IR ir = rule.getIR();
      SSAInstruction instruction = rule.getSSAInstruction();
      IVariable left = rule.getLeft();
      if (!(left instanceof LexicalVariable)) {
        left = new Variable(rule.getLeft().getName() + suffix);
      }
      List right = new ArrayList();
      for (Iterator j = rule.getRight().iterator(); j.hasNext(); ) {
        ISymbol s = (ISymbol) j.next();
        if (s instanceof IVariable && !(s instanceof LexicalVariable)) {
          IVariable r = new Variable(s.getName() + suffix);
          right.add(r);
        }
        else {
          right.add(s);
        }
      }
      GRule newRule = null;
      if (rule instanceof GInvocationRule) {
        GInvocationRule crule = (GInvocationRule) rule;
        GInvocationRule newCRule = new GInvocationRule(crule.getGrammars(), ir, instruction, left, right);
        newCRule.getAliasRules().addAll(crule.getAliasRules());
        newRule = newCRule;
      }
      else {
        newRule = new GRule(ir, instruction, left, right);
      }
      ruleMap.put(isym, newRule);
    }
    List parameters = new ArrayList();
    for (Iterator i = g.getParameterVariables().iterator(); i.hasNext(); ) {
      IVariable v = (IVariable) i.next();
      v = new Variable(v.getName() + suffix);
      parameters.add(v);
    }
    Set returns = new HashSet();
    for (Iterator i = g.getReturnSymbols().iterator(); i.hasNext(); ) {
      IVariable v = (IVariable) i.next();
      v = new Variable(v.getName() + suffix);
      returns.add(v);
    }
    return new GR(g.getIR(), parameters, returns, g.getAutomaton(), g.getFails(), ruleMap);
  }

  /**
   * replace all the variables
   * @param g
   * @param baseNames
   * @param m
   * @return
   */
  static public RegularlyControlledGrammar useUniqueVariables(RegularlyControlledGrammar g, IVariableFactory varFactory, final Map m) {
    Set variables = g.getNonterminals();
    for (Iterator i = variables.iterator(); i.hasNext(); ) {
      IVariable v = (IVariable) i.next();
      IVariable newVar = varFactory.createVariable(variablePrefix);
      m.put(v, newVar);
    }

    final Map ruleMap = new HashMap(g.getRuleMap());
    for (Iterator i = ruleMap.keySet().iterator(); i.hasNext(); ) {
      ISymbol isym = (ISymbol) i.next();
      IProductionRule rule = (IProductionRule) ruleMap.get(isym);
      IVariable left = (IVariable) m.get(rule.getLeft());
      List right = new ArrayList();
      for (Iterator j = rule.getRight().iterator(); j.hasNext(); ) {
        ISymbol s = (ISymbol) j.next();
        if (s instanceof IVariable) {
          right.add(m.get(s));
        }
        else {
          right.add(s);
        }
      }
      IProductionRule newRule = new ProductionRule(left, right);
      ruleMap.put(isym, newRule);
    }
    return new RegularlyControlledGrammar(g.getAutomaton(), g.getFails(), ruleMap);
  }

  static public void createSimpleUnion(IRegularlyControlledGrammar g1, IRegularlyControlledGrammar g2) {
    Automatons.createSimpleUnion(g1.getAutomaton(), g2.getAutomaton());
    g1.getFails().addAll(g2.getFails());
    g1.getRuleMap().putAll(g2.getRuleMap());
  }

  /**
   * create an union of the two regularly controlled grammar.
   * TODO: should refer formal definition of the union operator
   * @param g1
   * @param g2
   * @return an union of the two regularly controlled grammar
   */
  static public IRegularlyControlledGrammar createUnion(IRegularlyControlledGrammar g1, IRegularlyControlledGrammar g2) {
    Map m = new HashMap();
    g2 = useUniqueInputSymbols(g2, g1, m);
    g2 = useUniqueStates(g2, g1, m);
    g1 = (IRegularlyControlledGrammar) g1.copy(SimpleGrammarCopier.defaultCopier);
    createSimpleUnion(g1, g2);
    return g1;
  }

  /**
   * concatenate the two regularly controlled grammar without renaming in destructive manner.
   * @param g1
   * @param g2
   */
  static public void createSimpleConcatenation(IRegularlyControlledGrammar g1, IRegularlyControlledGrammar g2) {
    Automatons.createSimpleConcatenation(g1.getAutomaton(), g2.getAutomaton());
    g1.getFails().addAll(g2.getFails());
    g1.getRuleMap().putAll(g2.getRuleMap());
  }

  /**
   * concatenate the two regularly controlled grammar after renaming.
   * @param g1
   * @param g2
   * @return a concatenation of the two grammar
   */
  static public IRegularlyControlledGrammar createConcatenation(IRegularlyControlledGrammar g1, IRegularlyControlledGrammar g2) {
    Map m = new HashMap();
    g2 = useUniqueInputSymbols(g2, g1, m);
    g2 = useUniqueStates(g2, g1, m);
    g1 = (IRegularlyControlledGrammar) g1.copy(SimpleGrammarCopier.defaultCopier);
    createSimpleConcatenation(g1, g2);
    return g1;
  }

  /**
   * create a closure of the grammar.
   * @param g
   * @return a closure of the grammar
   */
  static public IRegularlyControlledGrammar createClosure(IRegularlyControlledGrammar g) {
    RegularlyControlledGrammar rcg = (RegularlyControlledGrammar) g.copy(new RegularlyControlledGrammar.DeepCopier(SimpleGrammarCopier.defaultCopier, SimpleSTSCopier.defaultCopier));
    IAutomaton automaton = rcg.getAutomaton();
    IState initState = automaton.getInitialState();
    for (Iterator i = automaton.getFinalStates().iterator(); i.hasNext(); ){
      IState finalState = (IState) i.next();
      ISymbol isym = Automatons.createUniqueInputSymbol(automaton);
      ITransition transition = new Transition(finalState, initState, isym);
      automaton.getTransitions().add(transition);
    }
    return rcg;
  }
  
  static public class RCGGraphvizLabelGenerator implements Automatons.IGraphvizLabelGenerator {
    private IRegularlyControlledGrammar rcg;
    public RCGGraphvizLabelGenerator(IRegularlyControlledGrammar rcg) {
      this.rcg = rcg;
    }
    
    public String getLabel(ITransition t) {
      ISymbol input = t.getInputSymbol();
      IProductionRule rule = rcg.getRule(input);
      if (rule != null) {
        return rule.toString();
      }
      else {
        return "";
      }
    }
  }
  
  static public String toGraphviz(IRegularlyControlledGrammar rcg) {
    return Automatons.toGraphviz(rcg.getAutomaton(), new RCGGraphvizLabelGenerator(rcg));
  }
  
  /*
   * TODO: should fix ControlledGrammars.compact().
   */
  static public void compact(IRegularlyControlledGrammar rcg) {
    IAutomaton automaton = rcg.getAutomaton();
    Set<ITransition> transitions = automaton.getTransitions();
    Map<ISymbol,IProductionRule> ruleMap = rcg.getRuleMap();
    for (ITransition t : transitions) {
      if(!ruleMap.containsKey(t.getInputSymbol())) {
        t.setInputSymbol(Transition.EpsilonSymbol);
      }
    }
    Automatons.eliminateEpsilonTransitions(automaton);
    System.out.println(Automatons.toGraphviz(automaton));
    for (ISymbol s : ruleMap.keySet()) {
      System.out.println(s + " : " + ruleMap.get(s));
    }
  }

  static public void inlineExpansion(GR gr) {
    Set<ITransition> transitions = new HashSet<ITransition>(gr.getAutomaton().getTransitions());
    for (ITransition t : transitions) {
      ISymbol input = t.getInputSymbol();
      GRule rule = gr.getRule(input);
      if (rule instanceof GInvocationRule) {
        GInvocationRule invoke = (GInvocationRule) rule;
        InvocationSymbol invokeSym = (InvocationSymbol) invoke.getRight(0);
        final Map<ISymbol,ISymbol> m = new HashMap<ISymbol,ISymbol>(invoke.getAliasMap());
        for (GR g : invoke.getGrammars()) {
          Set<ISymbol> retSyms = g.getReturnSymbols();
          for (ISymbol retSym : retSyms) {
            m.put(retSym, invoke.getLeft());
          }
          inlineExpansion(g);
          g = (GR) g.copy(new DeepGrammarCopier(
            new DeepRuleCopier(
              new DeepSymbolCopier(){
                public ISymbol copy(ISymbol s) {
                  if (m.containsKey(s)) {
                    return m.get(s);
                  }
                  else {
                    return super.copy(s);
                  }
                }
              })));
          g = (GR) ControlledGrammars.useUniqueStates(g, gr, new HashMap());
          g = (GR) ControlledGrammars.useUniqueInputSymbols(g, gr, new HashMap());
          ITransition bridge = new Transition(t.getPreState(), g.getAutomaton().getInitialState());
          gr.getAutomaton().getTransitions().add(bridge);
          Set<IState> finalStates = g.getAutomaton().getFinalStates();
          for (IState finalState : finalStates) {
            bridge = new Transition(finalState, t.getPostState());
            gr.getAutomaton().getTransitions().add(bridge);
          }
          gr.getAutomaton().getTransitions().addAll(g.getAutomaton().getTransitions());
          gr.assignRules(g.getRuleMap());
          gr.getAutomaton().getTransitions().remove(t);
          gr.getRuleMap().remove(t.getInputSymbol());
        }
      }
    }
  }
}
