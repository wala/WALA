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
import java.util.List;
import java.util.Set;

import com.ibm.wala.automaton.string.Automaton;
import com.ibm.wala.automaton.string.CharSymbol;
import com.ibm.wala.automaton.string.FilteredTransition;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;

public class Ucfirst extends Transducer {

  public Ucfirst(int target) {
    super(target);
  }

  public Ucfirst() {
    super();
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState initState = s0;
    IState s1 = new State("s1");
    Set finalStates = new HashSet();
    finalStates.add(s1);

    ITransition t0 = new FilteredTransition(s0, s1, v, new ISymbol[] { v },
        new FilteredTransition.IFilter() {
          public List invoke(ISymbol symbol, List outputs) {
            List resultOutputs = new ArrayList();
            CharSymbol c = (CharSymbol) outputs.get(0);
            resultOutputs.add(new CharSymbol(Character.toUpperCase(c
                .charValue())));
            return resultOutputs;
          }
        }, null);
    transitions.add(t0);
    ITransition t1 = new Transition(s1, s1, v, new ISymbol[] { v });
    transitions.add(t1);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

}