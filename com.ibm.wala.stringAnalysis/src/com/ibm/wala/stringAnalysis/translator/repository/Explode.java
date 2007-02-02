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

public class Explode extends Transducer {

  private int delimiterVarIndex;

  public Explode(int target, int delimiter) {
    super(target);
    this.delimiterVarIndex = delimiter;
  }

  public Explode(int delimiter) {
    super();
    this.delimiterVarIndex = delimiter;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Variable delimiterVar = (Variable) params.get(delimiterVarIndex);

    Set rules = (Set) grammar.getRules();
    Iterator rulesIte = rules.iterator();
    List delimiterList = new ArrayList();
    while (rulesIte.hasNext()) {
      ProductionRule rule = (ProductionRule) rulesIte.next();
      if (rule.getLeft().equals(delimiterVar)) {
        delimiterList = rule.getRight();
        break;
      }
    }

    // final CharSymbol delimiter = new CharSymbol(" ");
    final CharSymbol[] delimiter = (CharSymbol[]) delimiterList
        .toArray(new CharSymbol[0]);
    final CharSymbol firstSubDelimiter = delimiter[0];

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState s1 = new State("s1");
    IState s2 = new State("s2");
    IState initState = s0;
    Set finalStates = new HashSet();
    finalStates.add(s1);
    finalStates.add(s2);

    ITransition t0 = new Transition(s0, s1, Transition.EpsilonSymbol);
    ITransition t1 = new Transition(s0, s2, Transition.EpsilonSymbol);
    transitions.add(t0);
    transitions.add(t1);
    ITransition t2 = new FilteredTransition(s1, s1, v, new ISymbol[] { v },
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !symbol.equals(firstSubDelimiter);
            if (r)
              System.err.println("(in filtered transition1: accept " + symbol
                  + ")");
            return r;
          }
        });
    ITransition t3 = new FilteredTransition(s2, s2, v, new ISymbol[] {}, null,
        new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !symbol.equals(firstSubDelimiter);
            if (r)
              System.err.println("(in filtered transition2: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t2);
    transitions.add(t3);

    final int delimiterLen = delimiter.length;
    int stateIndex = 3;
    IState state1 = s1;
    IState state2 = s2;
    final IState startState1 = s1;
    final IState startState2 = s2;

    for (int i = 0; i < delimiterLen - 1; i++) {
      final IState firstDelimiterState1 = new State("s3");
      final IState firstDelimiterState2 = new State("s4");
      final int index = i;

      IState nextState = new State("s" + stateIndex);
      if (index == 0) {
        nextState = firstDelimiterState1;
      }
      transitions = generateTransition(transitions, state1, nextState,
        startState1, firstDelimiterState1, delimiter, index, false);
      state1 = nextState;
      finalStates.add(state1);
      stateIndex++;
      nextState = new State("s" + stateIndex);
      if (index == 0) {
        nextState = firstDelimiterState2;
      }
      transitions = generateTransition(transitions, state2, nextState,
        startState2, firstDelimiterState2, delimiter, index, true);
      state2 = nextState;
      finalStates.add(state1);
      stateIndex++;
    }

    IState nextState = new State("s" + stateIndex);
    final CharSymbol subDelimiter = delimiter[delimiterLen - 1];
    transitions = generateNextTransition(transitions, state1, nextState,
      subDelimiter);
    state1 = nextState;
    ITransition t4 = new Transition(state1, state1, v, new ISymbol[] {});
    stateIndex++;
    finalStates.add(state1);
    nextState = new State("s" + stateIndex);
    transitions = generateNextTransition(transitions, state2, nextState,
      subDelimiter);
    state2 = nextState;
    ITransition t5 = new Transition(state2, s0, Transition.EpsilonSymbol);
    finalStates.add(state2);
    transitions.add(t4);
    transitions.add(t5);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

  private Set generateTransition(Set transitions, IState state,
                                 IState nextState, IState startState,
                                 IState firstDelimiterState,
                                 final CharSymbol[] delimiter, final int index,
                                 final boolean isEpsilonState) {
    final CharSymbol firstSubDelimiter = delimiter[0];
    final CharSymbol subDelimiter = delimiter[index];
    final CharSymbol nextSubDelimiter = delimiter[index + 1];

    Variable v = new Variable("v");

    transitions = generateNextTransition(transitions, state, nextState,
      subDelimiter);

    ITransition ta = new FilteredTransition(nextState, startState, v,
        new ISymbol[] { v }, new FilteredTransition.IFilter() {
          public List invoke(ISymbol symbol, List outputs) {
            List newOutputs = new ArrayList();
            if (!isEpsilonState) {
              for (int j = 0; j < index + 1; j++) {
                newOutputs.add(delimiter[j]);
              }
              newOutputs.add(symbol);
            }
            return newOutputs;
          }
        }, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = (!symbol.equals(nextSubDelimiter))
                && (!symbol.equals(firstSubDelimiter));
            if (r)
              System.err.println("(in filtered transition3: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(ta);
    ITransition tb = new FilteredTransition(nextState, firstDelimiterState, v,
        new ISymbol[] { v }, new FilteredTransition.IFilter() {
          public List invoke(ISymbol symbol, List outputs) {
            List newOutputs = new ArrayList();
            if (!isEpsilonState) {
              for (int j = 0; j < index + 1; j++) {
                newOutputs.add(delimiter[j]);
              }
            }
            return newOutputs;
          }
        }, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = (!symbol.equals(nextSubDelimiter))
                && symbol.equals(firstSubDelimiter);
            if (r)
              System.err.println("(in filtered transition4: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(tb);
    return transitions;
  }

  private Set generateNextTransition(Set transitions, IState state,
                                     IState nextState,
                                     final CharSymbol subDelimiter) {
    Variable v = new Variable("v");
    ITransition t = new FilteredTransition(state, nextState, v,
        new ISymbol[] {}, null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = symbol.equals(subDelimiter);
            if (r)
              System.err.println("(in filtered transition5: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t);
    return transitions;
  }

}
