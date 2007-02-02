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
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;

public class Strtr extends Transducer {

  private int fromStrIndex;

  private int toStrIndex;

  public Strtr(int target, int from, int to) {
    super(target);
    fromStrIndex = from;
    toStrIndex = to;
  }

  public Strtr() {
    super();
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Variable fromSym = (Variable) params.get(fromStrIndex);
    Variable toSym = (Variable) params.get(toStrIndex);
    List fromList = new ArrayList();
    List toList = new ArrayList();
    Set rules = (Set) grammar.getRules();
    Iterator rulesIte = rules.iterator();
    while (rulesIte.hasNext()) {
      ProductionRule rule = (ProductionRule) rulesIte.next();
      IVariable left = rule.getLeft();
      if (left.equals(fromSym)) {
        fromList = rule.getRight();
      }
      else if (left.equals(toSym)) {
        toList = rule.getRight();
      }
    }
    CharSymbol[] toCharSyms = (CharSymbol[]) toList
        .toArray(new CharSymbol[0]);
    final List fromFinalList = fromList.subList(0, toCharSyms.length);
    Set transitions = new HashSet();
    IState initState = new State("s0");
    IState state = initState;
    Set finalStates = new HashSet();
    finalStates.add(state);

    for (int i = 0; i < toCharSyms.length; i++) {
      IState nextState = new State("s" + (i + 1));
      finalStates.add(nextState);
      ITransition t0 = new FilteredTransition(state, state, v,
          new ISymbol[] { v }, null, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = !(fromFinalList.contains(symbol));
              if (r)
                System.err.println("(in filtered transition: accept "
                    + symbol + ")");
              return r;
            }
          });
      transitions.add(t0);
      ITransition t1 = new FilteredTransition(state, nextState, v,
          new ISymbol[] { toCharSyms[i] }, null,
          new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = fromFinalList.contains(symbol);
              if (r)
                System.err.println("(in filtered transition: accept "
                    + symbol + ")");
              return r;
            }
          });
      transitions.add(t1);
      state = nextState;
    }
    ITransition t0 = new Transition(state, state, v, new ISymbol[] { v });
    transitions.add(t0);
    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }
}