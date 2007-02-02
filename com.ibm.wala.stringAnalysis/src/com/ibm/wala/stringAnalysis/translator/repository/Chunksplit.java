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
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.NumberSymbol;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.StringSymbol;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;

public class Chunksplit extends Transducer {

  private int chunkLenNumIndex;

  private int endStrIndex;

  public Chunksplit(int target, int len, int end) {
    super(target);
    this.chunkLenNumIndex = len;
    this.endStrIndex = end;
  }

  public Chunksplit(int len, int end) {
    this.chunkLenNumIndex = len;
    this.endStrIndex = end;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    int chunkLen = 76;
    List endStrList = new StringSymbol("\r\n").toCharSymbols();

    if (params.size() > chunkLenNumIndex) {
      NumberSymbol chunkLenNum = (NumberSymbol) params.get(chunkLenNumIndex);
      chunkLen = chunkLenNum.intValue();
    }
    if (params.size() > endStrIndex) {
      Variable endStrVar = (Variable) params.get(endStrIndex);
      Set rules = (Set) grammar.getRules();
      Iterator rulesIte = rules.iterator();

      while (rulesIte.hasNext()) {
        ProductionRule rule = (ProductionRule) rulesIte.next();
        if (rule.getLeft().equals(endStrVar)) {
          endStrList = rule.getRight();
          break;
        }
      }
    }

    final CharSymbol[] endStrChars = (CharSymbol[]) endStrList
        .toArray(new CharSymbol[0]);

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState initState = s0;
    Set finalStates = new HashSet();
    finalStates.add(s0);

    ITransition t0 = new Transition(s0, s0, v, new ISymbol[] { v });
    transitions.add(t0);
    ITransition t1 = new FilteredTransition(s0, s0, v, new ISymbol[] { v },
        new FilteredTransition.IFilter() {
          public List invoke(ISymbol symbol, List outputs) {
            List resultOutputs = new ArrayList();
            resultOutputs.add(symbol);
            for (int i = 0; i < endStrChars.length; i++) {
              resultOutputs.add(endStrChars[i]);
            }
            return resultOutputs;
          }
        }, null);
    transitions.add(t1);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }
}