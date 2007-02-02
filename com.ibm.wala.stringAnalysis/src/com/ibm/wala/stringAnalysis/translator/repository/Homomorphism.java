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
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Variable;

abstract public class Homomorphism extends Transducer {
  static protected interface Rule
  extends FilteredTransition.IFilter, FilteredTransition.ICondition {
  }

  public Homomorphism(int target) {
    super(target);
  }

  public Homomorphism() {
    super();
  }

  public IAutomaton getTransducer() {
    IState s = new State("s0");
    IVariable x = new Variable("x");
    Set<IState> finalStates = new HashSet<IState>();
    finalStates.add(s);
    Set<ITransition> transitions = new HashSet<ITransition>();
    for (Homomorphism.Rule r : getRuleSet()) {
      ITransition t = new FilteredTransition(s, s, x, new ISymbol[] { x }, r,
          r);
      transitions.add(t);
    }
    IAutomaton fst = new Automaton(s, finalStates, transitions);
    return fst;
  }

  abstract protected Set<Homomorphism.Rule> getRuleSet();
}