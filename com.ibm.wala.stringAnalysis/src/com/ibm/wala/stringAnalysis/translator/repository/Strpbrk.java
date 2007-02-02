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
/**
 * 
 */
package com.ibm.wala.stringAnalysis.translator.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.string.Automaton;
import com.ibm.wala.automaton.string.CharSymbol;
import com.ibm.wala.automaton.string.FilteredTransition;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IMatchContext;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;

public class Strpbrk extends Transducer {

  private int chrListVarIndex;

  public Strpbrk(int target, int chr) {
    super(target);
    this.chrListVarIndex = chr;
  }

  public Strpbrk(int chr) {
    super();
    this.chrListVarIndex = chr;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Variable chrListVar = (Variable) params.get(chrListVarIndex);
    Set rules = (Set) grammar.getRules();
    Iterator rulesIte = rules.iterator();
    List chrList = new ArrayList();
    while (rulesIte.hasNext()) {
      ProductionRule rule = (ProductionRule) rulesIte.next();
      if (rule.getLeft().equals(chrListVar)) {
        chrList = rule.getRight();
      }
    }
    final CharSymbol[] chrListChars = (CharSymbol[]) chrList
        .toArray(new CharSymbol[0]);

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState initState = s0;
    IState s1 = new State("s1");
    Set finalStates = new HashSet();
    finalStates.add(s0);
    finalStates.add(s1);

    ITransition t0 = new FilteredTransition(s0, s0, v, new ISymbol[] {},
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !containsCharList(symbol, chrListChars);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t0);
    ITransition t1 = new FilteredTransition(s0, s1, v, new ISymbol[] { v },
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = containsCharList(symbol, chrListChars);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t1);
    ITransition t2 = new Transition(s1, s1, v, new ISymbol[] { v });
    transitions.add(t2);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

  protected boolean containsCharList(ISymbol symbol, CharSymbol[] chrListChars) {
    for (int i = 0; i < chrListChars.length; i++) {
      if (symbol.equals(chrListChars[i])) {
        return true;
      }
    }
    return false;
  }

}