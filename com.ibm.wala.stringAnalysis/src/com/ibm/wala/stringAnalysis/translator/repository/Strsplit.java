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

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.automaton.string.Automaton;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.NumberSymbol;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;

public class Strsplit extends Transducer {

  private int splitLenNumIndex;

  public Strsplit(int target, int splitLen) {
    super(target);
    this.splitLenNumIndex = splitLen;
  }

  public Strsplit(int splitLen) {
    super();
    this.splitLenNumIndex = splitLen;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    int splitLen = 1;

    if (params.size() > splitLenNumIndex) {
      NumberSymbol splitLenNum = (NumberSymbol) params.get(splitLenNumIndex);
      splitLen = splitLenNum.intValue();
    }

    IState s0 = new State("s0");
    IState s1 = new State("s1");
    IState s2 = new State("s2");
    IState s3 = new State("s3");
    IState s4 = new State("s4");

    IState initState = s0;
    Set finalStates = new HashSet();
    Set transitions = new HashSet();

    ITransition t0 = new Transition(s0, s1, Transition.EpsilonSymbol);
    ITransition t1 = new Transition(s0, s2, Transition.EpsilonSymbol);
    transitions.add(t0);
    transitions.add(t1);
    
    IState state1 = s1;
    IState state2 = s2;
    int stateNumber = 3;
    for (int i = 0; i < splitLen; i++) {
      IState nextState1 = new State("s" + stateNumber);
      ITransition ta = new Transition(state1, nextState1, v, new ISymbol[] { v });
      transitions.add(ta);
      state1 = nextState1;
      finalStates.add(state1);
      stateNumber++;
      
      IState nextState2 = new State("s" + stateNumber);
      ITransition tb = new Transition(state2, nextState2, v, new ISymbol[] {});
      transitions.add(tb);
      state2 = nextState2;
      finalStates.add(state2);
      stateNumber++;
    }
    
    ITransition t3 = new Transition(state1, s0, Transition.EpsilonSymbol);
    transitions.add(t3);
    ITransition t4 = new Transition(state2, s0, Transition.EpsilonSymbol);
    transitions.add(t4);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

}
