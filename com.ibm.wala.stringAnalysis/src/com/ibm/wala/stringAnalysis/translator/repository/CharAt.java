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

public class CharAt extends Transducer {
  private int paramIndex;

  public CharAt(int target, int paramIndex) {
    super(target);
    this.paramIndex = paramIndex;
  }

  public CharAt(int paramIndex) {
    super();
    this.paramIndex = paramIndex;
  }

  public CharAt() {
    this(1);
  }

  public IAutomaton getTransducer() {
    ISymbol s = (ISymbol) params.get(paramIndex);
    if (s instanceof NumberSymbol) {
      NumberSymbol ns = (NumberSymbol) s;
      int idx = ns.intValue();
      IState initState = new State("s0");
      IState curState = initState;
      Set finalStates = new HashSet();
      Set transitions = new HashSet();
      int i;
      IState nextState = null;
      ITransition t = null;
      for (i = 0; i < idx; i++) {
        nextState = new State("s" + (i + 1));
        t = new Transition(curState, nextState, new Variable("c"));
        transitions.add(t);
        curState = nextState;
      }
      nextState = new State("s" + (i + 1));
      t = new Transition(curState, nextState, new Variable("c"),
          new ISymbol[] { new Variable("c") });
      transitions.add(t);
      t = new Transition(nextState, nextState, new Variable("c"));
      transitions.add(t);
      finalStates.add(nextState);

      IAutomaton a = new Automaton(initState, finalStates, transitions);
      return a;
    }
    else {
      return null;
    }
  }
}