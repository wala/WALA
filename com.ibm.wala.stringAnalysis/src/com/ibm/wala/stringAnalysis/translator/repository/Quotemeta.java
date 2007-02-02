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
import java.util.Iterator;
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
import com.ibm.wala.automaton.string.StringSymbol;
import com.ibm.wala.automaton.string.Variable;

public class Quotemeta extends Transducer {

  public Quotemeta(int target) {
    super(target);
  }

  public Quotemeta() {
    super();
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    final List quoteChars = new StringSymbol(".\\+*?[^]($)").toCharSymbols();

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState initState = s0;
    Set finalStates = new HashSet();
    finalStates.add(s0);

    CharSymbol backSlash = new CharSymbol("\\");
    ITransition t0 = new FilteredTransition(s0, s0, v, new ISymbol[] {
      backSlash, v }, null, new FilteredTransition.ICondition() {
      public boolean accept(ISymbol symbol, IMatchContext ctx) {
        boolean r = containsQuoteStr(symbol, quoteChars);
        if (r)
          System.err.println("(in filtered transition: accept " + symbol
              + ")");
        return r;
      }
    });
    transitions.add(t0);
    ITransition t1 = new FilteredTransition(s0, s0, v, new ISymbol[] { v },
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !containsQuoteStr(symbol, quoteChars);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t1);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

  protected boolean containsQuoteStr(ISymbol symbol, List quoteChars) {
    Iterator quoteIte = quoteChars.iterator();
    while (quoteIte.hasNext()) {
      CharSymbol quoteChar = (CharSymbol) quoteIte.next();
      if (symbol.equals(quoteChar)) {
        return true;
      }
    }
    return false;
  }
}