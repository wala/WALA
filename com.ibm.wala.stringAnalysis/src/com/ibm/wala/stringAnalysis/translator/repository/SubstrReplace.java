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

public class SubstrReplace extends Transducer {
  private int begParamIndex;

  private int lenParamIndex;

  private int repParamIndex;

  public SubstrReplace(int target, int rep, int beg, int len) {
    super(target);
    repParamIndex = rep;
    begParamIndex = beg;
    lenParamIndex = len;
  }

  public SubstrReplace(int rep, int beg, int len) {
    repParamIndex = rep;
    begParamIndex = beg;
    lenParamIndex = len;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    NumberSymbol symBeg = (NumberSymbol) params.get(begParamIndex);
    NumberSymbol symNum = null;

    Set rules = (Set) grammar.getRules();
    Iterator rulesIte = rules.iterator();

    if (params.size() > lenParamIndex) {
      ISymbol lenParam = (ISymbol) params.get(lenParamIndex);
      if (lenParam instanceof NumberSymbol) {
        symNum = (NumberSymbol) params.get(lenParamIndex);
      }
      else {
        while (rulesIte.hasNext()) {
          ProductionRule rule = (ProductionRule) rulesIte.next();
          if (rule.getLeft().equals(lenParam)) {
            NumberSymbol lenNumSym = (NumberSymbol) rule.getRight().get(0);
            symNum = new NumberSymbol(lenNumSym.intValue() * -1);
            break;
          }
        }
      }
    }
    Variable strRep = (Variable) params.get(repParamIndex);

    int beg = symBeg.intValue();
    List rep = new ArrayList();

    rulesIte = rules.iterator();
    while (rulesIte.hasNext()) {
      ProductionRule rule = (ProductionRule) rulesIte.next();
      if (rule.getLeft().equals(strRep)) {
        rep = rule.getRight();
        break;
      }
    }
    Set transitions = new HashSet();
    IState initState = new State("s0");
    IState state = initState;
    int i = 0;
    while (i < beg) {
      IState nextState = new State("s" + (i + 1));
      ITransition t = new Transition(state, nextState, v, new ISymbol[] { v });
      transitions.add(t);
      state = nextState;
      i++;
    }
    while (i == beg) {
      IState nextState = new State("s" + (i + 1));
      final List finalRep = rep;
      final NumberSymbol finalSymNum = symNum;
      ITransition t0 = new FilteredTransition(state, nextState, v,
          new ISymbol[] { v }, new FilteredTransition.IFilter() {
            public List invoke(ISymbol symbol, List outputs) {
              List resultOutputs = new ArrayList();
              Iterator finalRepIte = finalRep.iterator();
              while (finalRepIte.hasNext()) {
                resultOutputs.add(finalRepIte.next());
              }
              resultOutputs.add(outputs.get(0));
//              Iterator ite = resultOutputs.iterator();
              return resultOutputs;
            }
          }, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              int num = finalSymNum.intValue();
              boolean r = ((finalSymNum != null) && ((num == 0) || (num < 0)));
              if (r)
                System.err.println("(in filtered transition2: accept "
                    + symbol + ")");
              return r;
            }
          });
      transitions.add(t0);
      CharSymbol[] repCharSyms = (CharSymbol[]) rep
          .toArray(new CharSymbol[0]);
      ITransition t1 = new FilteredTransition(state, nextState, v,
          repCharSyms, null, new FilteredTransition.ICondition() {
            public boolean accept(ISymbol symbol, IMatchContext ctx) {
              boolean r = (finalSymNum == null)
                  || (finalSymNum.intValue() != 0);
              if (r)
                System.err.println("(in filtered transition2: accept "
                    + symbol + ")");
              return r;
            }
          });
      transitions.add(t1);
      state = nextState;
      i++;
    }
    // i > beg
    if (symNum != null) {
      int num = symNum.intValue();
      if (num >= 0) {
        while (i < beg + num) {
          IState nextState = new State("s" + (i + 1));
          ITransition t = new Transition(state, nextState, v,
              new ISymbol[] {});
          transitions.add(t);
          state = nextState;
          i++;
        }
        transitions.add(new Transition(state, state, v, new ISymbol[] { v }));
      }
      else {
        IState nextState = new State("s" + (i + 1));
        ITransition ta = new Transition(state, state, v, new ISymbol[] {});
        transitions.add(ta);
        ITransition tb = new Transition(state, nextState, v,
            new ISymbol[] { v });
        transitions.add(tb);
        ITransition tc = new Transition(nextState, nextState, v,
            new ISymbol[] { v });
        transitions.add(tc);
        state = nextState;
      }
    }
    else {
      transitions.add(new Transition(state, state, v, new ISymbol[] {}));
    }

    Set finalStates = new HashSet();
    finalStates.add(state);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }
}