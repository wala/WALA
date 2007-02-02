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
import com.ibm.wala.automaton.string.FilteredTransition;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IMatchContext;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.NumberSymbol;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;

public class Substr extends Transducer {
  private int begParamIndex;

  private int lenParamIndex;

  public Substr(int target, int beg, int n) {
    super(target);
    begParamIndex = beg;
    lenParamIndex = n;
  }

  public Substr(int beg, int n) {
    super();
    begParamIndex = beg;
    lenParamIndex = n;
  }

  public Substr() {
    this(1, 2);
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");
    FilteredTransition.ICondition anyChar = new FilteredTransition.ICondition() {
      public boolean accept(ISymbol symbol, IMatchContext ctx) {
        return !(symbol instanceof IVariable);
      }
    };

    NumberSymbol symBeg = (NumberSymbol) params.get(begParamIndex);
    NumberSymbol symNum = (NumberSymbol) params.get(lenParamIndex);

    int beg = symBeg.intValue();
    int num = symNum.intValue();

    Set transitions = new HashSet();
    IState initState = new State("s0");
    IState state = initState;
    int i = 0;
    while (i < beg) {
      IState nextState = new State("s" + (i + 1));
      ITransition t = new FilteredTransition(state, nextState, v,
          new ISymbol[0], null, anyChar);
      transitions.add(t);
      state = nextState;
      i++;
    }
    while (i < beg + num) {
      IState nextState = new State("s" + (i + 1));
      ITransition t = new Transition(state, nextState, v, new ISymbol[] { v });
      transitions.add(t);
      state = nextState;
      i++;
    }
    transitions.add(new Transition(state, state, v, new ISymbol[] {}));

    Set finalStates = new HashSet();
    finalStates.add(state);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }
}