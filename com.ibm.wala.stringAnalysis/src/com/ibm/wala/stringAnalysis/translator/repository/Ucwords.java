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
import com.ibm.wala.automaton.string.IMatchContext;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Variable;

public class Ucwords extends Transducer {

  public Ucwords(int target) {
    super(target);
  }

  public Ucwords() {
    super();
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState initState = s0;
    IState s1 = new State("s1");
    Set finalStates = new HashSet();
    finalStates.add(s0);
    finalStates.add(s1);

    ITransition t0 = new FilteredTransition(s0, s0, v, new ISymbol[] { v },
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = containsSpecialSymbol(symbol);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t0);
    ITransition t1 = new FilteredTransition(s0, s1, v, new ISymbol[] { v },
        new FilteredTransition.IFilter() {
          public List invoke(ISymbol symbol, List outputs) {
            List resultOutputs = new ArrayList();
            CharSymbol c = (CharSymbol) symbol;
            resultOutputs.add(new CharSymbol(Character.toUpperCase(c
                .charValue())));
            return resultOutputs;
          }
        }, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !containsSpecialSymbol(symbol);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t1);
    ITransition t2 = new FilteredTransition(s1, s1, v, new ISymbol[] { v },
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !containsSpecialSymbol(symbol);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t2);
    ITransition t3 = new FilteredTransition(s1, s0, v, new ISymbol[] { v },
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = containsSpecialSymbol(symbol);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t3);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

  protected boolean containsSpecialSymbol(ISymbol symbol) {
    final ISymbol spaceChar = new CharSymbol(' ');
    final ISymbol newLineChar = new CharSymbol('\n');
    final ISymbol carriageRetChar = new CharSymbol('\r');
    final ISymbol horizontalTabChar = new CharSymbol('\t');

    if (symbol.equals(spaceChar) || symbol.equals(newLineChar)
        || symbol.equals(carriageRetChar) || symbol.equals(horizontalTabChar)) {
      return true;
    }
    return false;
  }

}