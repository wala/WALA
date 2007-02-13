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

public class Strchr extends Transducer {

  private int needleVarIndex;

  public Strchr(int target, int needle) {
    super(target);
    this.needleVarIndex = needle;
  }

  public Strchr(int needle) {
    super();
    this.needleVarIndex = needle;
  }

  public IAutomaton getTransducer() {

    Variable v = new Variable("v");

    Variable needleVar = (Variable) params.get(needleVarIndex);

    Set rules = (Set) grammar.getRules();
    Iterator rulesIte = rules.iterator();
    List needleList = new ArrayList();
    while (rulesIte.hasNext()) {
      ProductionRule rule = (ProductionRule) rulesIte.next();
      if (rule.getLeft().equals(needleVar)) {
        needleList = rule.getRight();
        break;
      }
    }

    final CharSymbol needleChar = (CharSymbol) needleList.get(0);

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState s1 = new State("s1");
    IState initState = s0;
    Set finalStates = new HashSet();
    finalStates.add(s1);

    ITransition t0 = new FilteredTransition(s0, s0, v, new ISymbol[] { v },
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !symbol.equals(needleChar);
            if (r)
              System.err.println("(in filtered transition1: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t0);

    ITransition t1 = new FilteredTransition(s0, s1, v, new ISymbol[] { v },
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = symbol.equals(needleChar);
            if (r)
              System.err.println("(in filtered transition2: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t1);
    ITransition t2 = new Transition(s1, s1, v, new ISymbol[] {});
    transitions.add(t2);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }
}
