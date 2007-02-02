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
import com.ibm.wala.automaton.string.NumberSymbol;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;

public class Wordwrap extends Transducer {

  private int breakStrIndex;

  private int widthNumIndex;

  private int cutNumIndex;

  public Wordwrap(int target, int width, int breakStr, int cut) {
    super(target);
    this.breakStrIndex = breakStr;
    this.widthNumIndex = width;
    this.cutNumIndex = cut;
  }

  public Wordwrap(int breakStr, int width, int cut) {
    super();
    this.breakStrIndex = breakStr;
    this.widthNumIndex = width;
    this.cutNumIndex = cut;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Variable breakStrVar = (Variable) params.get(breakStrIndex);

    int cutNum = 0;
    if (params.size() > cutNumIndex) {
      cutNum = ((NumberSymbol) params.get(cutNumIndex)).intValue();
    }
    final CharSymbol spaceChar = new CharSymbol(" ");

    Set rules = (Set) grammar.getRules();
    Iterator rulesIte = rules.iterator();
    List breakStrList = new ArrayList();
    while (rulesIte.hasNext()) {
      ProductionRule rule = (ProductionRule) rulesIte.next();
      if (rule.getLeft().equals(breakStrVar)) {
        breakStrList = rule.getRight();
        break;
      }
    }
    final CharSymbol[] breakStrChars = (CharSymbol[]) breakStrList
        .toArray(new CharSymbol[0]);

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState initState = s0;
    Set finalStates = new HashSet();
    finalStates.add(s0);

    if (cutNum == 0) {
      ITransition t0 = new Transition(s0, s0, v, new ISymbol[] { v });
      transitions.add(t0);
      ITransition t1 = new FilteredTransition(s0, s0, v, breakStrChars, null,
          new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = symbol.equals(spaceChar);
              if (r)
                System.err.println("(symbol.equals(spaceChar): accept "
                    + symbol + ")");
              return r;
            }
          });
      transitions.add(t1);
    }
    else {
      int width = ((NumberSymbol) params.get(widthNumIndex)).intValue();
      IState s1 = new State("s1");
      ITransition t0 = new Transition(s0, s1, v, new ISymbol[] { v });
      transitions.add(t0);
      ITransition t1 = new FilteredTransition(s0, s0, v, breakStrChars, null,
          new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = symbol.equals(spaceChar);
              if (r)
                System.err.println("(symbol.equals(spaceChar): accept "
                    + symbol + ")");
              return r;
            }
          });
      transitions.add(t1);
      finalStates.add(s1);
      IState state = s1;
      for (int i = 1; i < width - 1; i++) {
        IState nextState = new State("s" + (i + 1));
        ITransition t2 = new Transition(state, nextState, v,
            new ISymbol[] { v });
        transitions.add(t2);
        ITransition t3 = new FilteredTransition(state, s0, v, breakStrChars,
            null, new FilteredTransition.ICondition() {
              public boolean accept(ISymbol symbol, IMatchContext ctx) {
                boolean r = symbol.equals(spaceChar);
                if (r)
                  System.err.println("(symbol.equals(spaceChar): accept "
                      + symbol + ")");
                return r;
              }
            });
        transitions.add(t3);
        state = nextState;
        finalStates.add(state);
      }
      IState nextState = new State("s" + (width + 1));
      ITransition t4 = new Transition(state, nextState, v, new ISymbol[] { v });
      transitions.add(t4);
      ITransition t5 = new FilteredTransition(nextState, s1, v, new ISymbol[] { v },
          new FilteredTransition.IFilter() {
            public List invoke(ISymbol symbol, List outputs) {
              List newOutputs = new ArrayList();
              for (int i = 0; i < breakStrChars.length; i++) {
                newOutputs.add(breakStrChars[i]);
              }
              newOutputs.add(symbol);
              return newOutputs;
            }
          }, null);
      transitions.add(t5);
      finalStates.add(nextState);
    }

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }
}