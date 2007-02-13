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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.automaton.string.Automaton;
import com.ibm.wala.automaton.string.Automatons;
import com.ibm.wala.automaton.string.FilteredTransition;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IMatchContext;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.StringSymbol;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;

public class Split extends Transducer {
//  private int sepIndex;

//  private int limIndex;

//  static private int defaultLimIndex = 2;

  public Split(int target, int sepIndex, int limIndex) {
    super(target);
//    this.sepIndex = sepIndex;
//    this.limIndex = limIndex;
  }

  public Split(int target, int sepIndex) {
    super(target);
//    this.sepIndex = sepIndex;
//    this.limIndex = defaultLimIndex;
  }

  public Split(int sepIndex) {
    super();
//    this.sepIndex = sepIndex;
//    this.limIndex = defaultLimIndex;
  }

  public Split() {
    this(1);
  }

  public IAutomaton getTransducer() {
//    ISymbol sepSym = (ISymbol) params.get(sepIndex);
    String sep = ":"; // TODO: should obtain this from 'sepSym'.
    // ISymbol limSym = (ISymbol) params.get(limIndex);
    IState s0 = new State("s0");
    IState s1 = new State("s1");
    IState s2 = new State("s2");
    IState initState = s0;
    Set finalStates = new HashSet();
    Set transitions = new HashSet();

    final List sepChars = (new StringSymbol(sep)).toCharSymbols();
    IAutomaton a1 = Automatons.createAutomaton(sepChars);

    final ISymbol sepChars0 = (ISymbol) sepChars.get(0);
    ITransition t0 = new Transition(s0, s1, Transition.EpsilonSymbol);
    ITransition t1 = new FilteredTransition(s1, s1, new Variable("c"),
        new ISymbol[] { new Variable("c") }, null,
        new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !symbol.equals(sepChars0);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    ITransition t2 = new Transition(s0, s2, Transition.EpsilonSymbol);
    ITransition t3 = new FilteredTransition(s2, s2, new Variable("c"),
        new ISymbol[] {}, null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !symbol.equals(sepChars0);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t0);
    transitions.add(t1);
    transitions.add(t2);
    transitions.add(t3);
    finalStates.add(s2);
    IAutomaton a2 = new Automaton(initState, finalStates, transitions);

    Map m = new HashMap();
    IAutomaton a3 = Automatons.createConcatenation(a2, a1, m);
    for (Iterator i = a3.getFinalStates().iterator(); i.hasNext();) {
      IState fin = (IState) i.next();
      ITransition tt1 = new Transition(fin, s1, Transition.EpsilonSymbol);
      a3.getTransitions().add(tt1);
      ITransition tt2 = new Transition(fin, s0, Transition.EpsilonSymbol);
      a3.getTransitions().add(tt2);
    }

    a3.getFinalStates().clear();
    IState finalState = Automatons.createUniqueState(a3);
    ITransition t = new Transition(s1, finalState, Transition.EpsilonSymbol);
    a3.getFinalStates().add(finalState);
    a3.getTransitions().add(t);
    ITransition anyChar = new Transition(finalState, finalState,
        new Variable("a"));
    a3.getTransitions().add(anyChar);
    Automatons.eliminateEpsilonTransitions(a3);
    System.err.println(Automatons.toGraphviz(a3));
    System.err.println(a3.translate(StringSymbol.toCharSymbols("a:b:c")));
    return a3;
  }
}