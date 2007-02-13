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
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;

public class Stristr extends Transducer {

  private int needleStrIndex;

  public Stristr(int target, int needle) {
    super(target);
    this.needleStrIndex = needle;
  }

  public Stristr(int needle) {
    super();
    this.needleStrIndex = needle;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState initState = s0;
    Set finalStates = new HashSet();
    finalStates.add(s0);

    final Variable needleVar = (Variable) params.get(needleStrIndex);
    Set rules = (Set) grammar.getRules();
    Iterator rulesIte = rules.iterator();
    List needleList = new ArrayList();
    while (rulesIte.hasNext()) {
      ProductionRule rule = (ProductionRule) rulesIte.next();
      if (rule.getLeft().equals(needleVar)) {
        needleList = rule.getRight();
      }
    }
    final CharSymbol[] needleChars = (CharSymbol[]) needleList
        .toArray(new CharSymbol[0]);

    ITransition t0 = new FilteredTransition(s0, s0, v, new ISymbol[] {}, null,
        new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !isEquals(symbol, needleChars[0]);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t0);
    IState state = s0;
    int lastIndex = needleChars.length;
    for (int i = 0; i < lastIndex - 1; i++) {
      IState nextState = new State("s" + (i + 1));
      final CharSymbol needleChar = needleChars[i];
//      final CharSymbol nextNeedleChar = needleChars[i + 1];
      ITransition ta = new FilteredTransition(state, nextState, v,
          new ISymbol[] {}, null, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = isEquals(symbol, needleChar);
              if (r)
                System.err.println("(in filtered transition: accept " + symbol
                    + ")");
              return r;
            }
          });
      transitions.add(ta);
      ITransition tb = new Transition(nextState, s0, Transition.EpsilonSymbol);
      transitions.add(tb);
      state = nextState;
      finalStates.add(state);
    }

    IState nextState = new State("s" + lastIndex);
    List charSymbolsList = getCharSymbolsList(needleList);
    Iterator charSymbolsIte = charSymbolsList.iterator();
    while (charSymbolsIte.hasNext()) {
      CharSymbol[] newNeedleChars = (CharSymbol[]) charSymbolsIte
          .next();
      final CharSymbol needleChar = needleChars[lastIndex - 1];
      ITransition t = new FilteredTransition(state, nextState, v, newNeedleChars,
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = isEquals(symbol, needleChar);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
      transitions.add(t);
    }
    state = nextState;
    finalStates.add(state);
    ITransition t1 = new Transition(state, state, v, new ISymbol[] { v });
    transitions.add(t1);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

  private List getCharSymbolsList(List needleList) {
    List charSymbolsList = new ArrayList();
    CharSymbol[] symbols = (CharSymbol[]) needleList.toArray(new CharSymbol[0]);
    for (int i = 0; i < (int) Math.pow(2.0, symbols.length); i++) {
      CharSymbol[] result = new CharSymbol[symbols.length];
      for (int index = 0, length = symbols.length; index < length; index++) {
        char chr = (i & 1 << (length - 1 - index)) > 0 ? Character
            .toUpperCase(symbols[index].charValue()) : Character
            .toLowerCase(symbols[index].charValue());
        result[index] = new CharSymbol(chr);
      }
      charSymbolsList.add(result);
    }
    return charSymbolsList;
  }

  protected boolean isEquals(ISymbol symbol, CharSymbol needleChar) {
    CharSymbol uChar = new CharSymbol(Character.toUpperCase(needleChar
        .charValue()));
    CharSymbol lChar = new CharSymbol(Character.toLowerCase(needleChar
        .charValue()));
    if ((symbol.equals(uChar)) || (symbol.equals(lChar))) {
      return true;
    }
    return false;
  }
}