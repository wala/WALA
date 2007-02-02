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
import com.ibm.wala.automaton.string.Variable;

public class Strrot13 extends Transducer {

  public Strrot13(int target) {
    super(target);
  }

  public Strrot13() {
    super();
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState initState = s0;
    Set finalStates = new HashSet();
    finalStates.add(s0);

    ITransition t1 = new FilteredTransition(s0, s0, v, new ISymbol[] { v },
        new FilteredTransition.IFilter() {
          public List invoke(ISymbol symbol, List outputs) {
            List resultOutputs = new ArrayList();
            if (symbol instanceof CharSymbol) {
              char sym = ((CharSymbol) symbol).charValue();
              if (Character.isLetter(sym)) {
                if (Character.isLowerCase(sym)) {
                  return generateOutputs("a", "z", sym);
                }
                return generateOutputs("A", "Z", sym);
              }
            }
            return outputs;
          }

          private List generateOutputs(String start, String end, char sym) {
            List outputs = new ArrayList();
            CharSymbol newSym = new CharSymbol((char) (sym + 13));
            int endVal = new CharSymbol(end).charValue();
            int symToEnd = endVal - sym;
            if (symToEnd < 13) {
              int startVal = new CharSymbol(start).charValue();
              newSym = new CharSymbol((char) (startVal + 12 - symToEnd));
            }
            outputs.add(newSym);
            return outputs;
          }
        }, null);
    transitions.add(t1);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }
}