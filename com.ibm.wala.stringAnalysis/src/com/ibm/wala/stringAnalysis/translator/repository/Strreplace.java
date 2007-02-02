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
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Variable;

public class Strreplace extends Transducer {

  private int repStrIndex;

  private int searchStrIndex;

  public Strreplace(int target, int search, int replace) {
    super(target);
    this.repStrIndex = replace;
    this.searchStrIndex = search;
  }

  public Strreplace(int search, int replace) {
    super();
    this.repStrIndex = replace;
    this.searchStrIndex = search;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Variable repStrVar = (Variable) params.get(repStrIndex);
    Variable searchStrVar = (Variable) params.get(searchStrIndex);

    Set rules = (Set) grammar.getRules();
    Iterator rulesIte = rules.iterator();
    List repStrList = new ArrayList();
    List searchStrList = new ArrayList();
    while (rulesIte.hasNext()) {
      ProductionRule rule = (ProductionRule) rulesIte.next();
      IVariable left = rule.getLeft();
      List right = rule.getRight();
      if (left.equals(repStrVar)) {
        repStrList = right;
      }
      else if (left.equals(searchStrVar)) {
        searchStrList = right;
      }
    }

    final CharSymbol[] repChars = (CharSymbol[]) repStrList
        .toArray(new CharSymbol[0]);
    final CharSymbol[] searchChars = (CharSymbol[]) searchStrList
        .toArray(new CharSymbol[0]);

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState initState = s0;
    Set finalStates = new HashSet();
    finalStates.add(s0);

    ITransition t0 = new FilteredTransition(s0, s0, v, new ISymbol[] { v },
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !symbol.equals(searchChars[0]);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t0);

    IState state = s0;
    IState searchFirstState = new State("s1");
    final CharSymbol searchFirstChar = searchChars[0];
    final int searchCharsLen = searchChars.length;
    for (int i = 0; i < searchCharsLen - 1; i++) {
      IState nextState = new State("s" + (i + 1));
      if (i == 0) {
        nextState = searchFirstState;
      }
      final CharSymbol searchChar = searchChars[i];
      final CharSymbol nextSearchChar = searchChars[i + 1];
      final int index = i;
      ITransition ta = new FilteredTransition(state, nextState, v,
          new ISymbol[] {}, null, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = symbol.equals(searchChar);
              if (r)
                System.err.println("(in filtered transition: accept " + symbol
                    + ")");
              return r;
            }
          });
      transitions.add(ta);
      ITransition tb = new FilteredTransition(nextState, s0, v,
          new ISymbol[] { v }, new FilteredTransition.IFilter() {
            public List invoke(ISymbol symbol, List outputs) {
              List newOutputs = new ArrayList();
              for (int j = 0; j < index + 1; j++) {
                newOutputs.add(searchChars[j]);
              }
              newOutputs.add(symbol);
              return newOutputs;
            }
          }, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = (!symbol.equals(nextSearchChar))
                  && (!symbol.equals(searchFirstChar));
              if (r)
                System.err.println("(in filtered transition: accept " + symbol
                    + ")");
              return r;
            }
          });
      transitions.add(tb);
      ITransition tc = new FilteredTransition(nextState, searchFirstState, v,
          new ISymbol[] { v }, new FilteredTransition.IFilter() {
            public List invoke(ISymbol symbol, List outputs) {
              List newOutputs = new ArrayList();
              for (int j = 0; j < index + 1; j++) {
                newOutputs.add(searchChars[j]);
              }
              return newOutputs;
            }
          }, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = (!symbol.equals(nextSearchChar))
                  && symbol.equals(searchFirstChar);
              if (r)
                System.err.println("(in filtered transition: accept " + symbol
                    + ")");
              return r;
            }
          });
      transitions.add(tc);
      state = nextState;
      finalStates.add(state);
    }

    ITransition t1 = new FilteredTransition(state, s0, v, repChars, null,
        new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = symbol.equals(searchChars[searchCharsLen - 1]);
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
}