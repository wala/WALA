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
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.StringSymbol;
import com.ibm.wala.automaton.string.Variable;

public class Htmlspecialchars extends Transducer {

  private int quoteStyIndex;

  public Htmlspecialchars(int target, int quote) {
    super(target);
    this.quoteStyIndex = quote;
  }

  public Htmlspecialchars(int quote) {
    super();
    this.quoteStyIndex = quote;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    final ISymbol ampersandChar = new CharSymbol('&');
    final ISymbol lessThanChar = new CharSymbol('<');
    final ISymbol greaterThanChar = new CharSymbol('>');
    final ISymbol doubleQuoteChar = new CharSymbol('\"');
    final ISymbol singleQuoteChar = new CharSymbol('\'');

    String quoteType = "";
    if (params.size() > quoteStyIndex) {
      Variable quoteStyVar = (Variable) params.get(quoteStyIndex);
      List quoteStyList = new ArrayList();
      Set rules = (Set) grammar.getRules();
      Iterator rulesIte = rules.iterator();
      while (rulesIte.hasNext()) {
        ProductionRule varRule = (ProductionRule) rulesIte.next();
        if (varRule.getLeft().equals(quoteStyVar)) {
          quoteStyList = varRule.getRight();
        }
      }
      Iterator quoteStyIte = quoteStyList.iterator();
      while (quoteStyIte.hasNext()) {
        CharSymbol c = (CharSymbol) quoteStyIte.next();
        quoteType += c.charValue();
      }
    }
    else {
      quoteType = "ENT_COMPAT";
    }
    final String finalQuoteType = quoteType;

    Set transitions = new HashSet();
    IState s0 = new State("s0");
    IState initState = s0;
    Set finalStates = new HashSet();
    finalStates.add(s0);

    ITransition t0 = new FilteredTransition(s0, s0, v, new ISymbol[] { v },
        new FilteredTransition.IFilter() {
          public List invoke(ISymbol symbol, List outputs) {
//            List resultOutputs = new ArrayList();
            CharSymbol c = (CharSymbol) outputs.get(0);
            if (c.equals(ampersandChar)) {
              return new StringSymbol("&amp;").toCharSymbols();
            }
            if (c.equals(lessThanChar)) {
              return new StringSymbol("&lt;").toCharSymbols();
            }
            if (c.equals(greaterThanChar)) {
              return new StringSymbol("&gt;").toCharSymbols();
            }
            if (c.equals(doubleQuoteChar)
                && (finalQuoteType.equals("ENT_QUOTES") || finalQuoteType
                    .equals("ENT_COMPAT"))) {
              return new StringSymbol("&quot;").toCharSymbols();
            }
            if (c.equals(singleQuoteChar)
                && finalQuoteType.equals("ENT_QUOTES")) {
              return new StringSymbol("&#039;").toCharSymbols();
            }
            return outputs;
          }
        }, null);
    transitions.add(t0);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }
}