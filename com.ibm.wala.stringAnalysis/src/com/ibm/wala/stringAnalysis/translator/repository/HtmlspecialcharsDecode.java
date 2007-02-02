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
import com.ibm.wala.automaton.string.StringSymbol;
import com.ibm.wala.automaton.string.Variable;

public class HtmlspecialcharsDecode extends Transducer {

  private int quoteStyIndex;

  public HtmlspecialcharsDecode(int target, int quote) {
    super(target);
    this.quoteStyIndex = quote;
  }

  public HtmlspecialcharsDecode(int quote) {
    super();
    this.quoteStyIndex = quote;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    final ISymbol ampersandChar = new CharSymbol('&');
    CharSymbol[] ampersandChars = (CharSymbol[]) new StringSymbol("&&amp;")
        .toCharSymbols().toArray(new CharSymbol[0]);
    CharSymbol[] lessThanChars = (CharSymbol[]) new StringSymbol("<&lt;")
        .toCharSymbols().toArray(new CharSymbol[0]);
    CharSymbol[] greaterThanChars = (CharSymbol[]) new StringSymbol(">&gt;")
        .toCharSymbols().toArray(new CharSymbol[0]);
    CharSymbol[] doubleQuoteChars = (CharSymbol[]) new StringSymbol(
        "\"&quot;").toCharSymbols().toArray(new CharSymbol[0]);
    CharSymbol[] singleQuoteChars = (CharSymbol[]) new StringSymbol(
        "\'&#039;").toCharSymbols().toArray(new CharSymbol[0]);

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

    List charSymbolArrayList = new ArrayList();
    charSymbolArrayList.add(ampersandChars);
    charSymbolArrayList.add(lessThanChars);
    charSymbolArrayList.add(greaterThanChars);
    if (quoteType.equals("ENT_COMPAT")) {
      charSymbolArrayList.add(doubleQuoteChars);
    }
    if (quoteType.equals("ENT_QUOTES")) {
      charSymbolArrayList.add(doubleQuoteChars);
      charSymbolArrayList.add(singleQuoteChars);
    }
    final CharSymbol[] secondChars = getSecondChars(charSymbolArrayList);

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
            boolean r = !symbol.equals(ampersandChar);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t0);
    ITransition t1 = new FilteredTransition(s0, s1, v, new ISymbol[] {},
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = symbol.equals(ampersandChar);
            if (r)
              System.err.println("(in filtered transition: accept " + symbol
                  + ")");
            return r;
          }
        });
    transitions.add(t1);
    ITransition t2 = new FilteredTransition(s1, s0, v, new ISymbol[] {
      ampersandChar, v }, null, new FilteredTransition.ICondition() {
      public boolean accept(ISymbol symbol, IMatchContext ctx) {
        boolean r = !containsChar(symbol, secondChars);
        if (r)
          System.err.println("(in filtered transition: accept " + symbol
              + ")");
        return r;
      }
    });
    transitions.add(t2);
    IState state = s1;
    IState nextState = s1;
    int i = 1;
    Iterator charSymIte = charSymbolArrayList.iterator();
    while (charSymIte.hasNext()) {
      final CharSymbol[] charSym = (CharSymbol[]) charSymIte.next();
      for (int j = 2; j < charSym.length; j++) {
        final CharSymbol finalChar = charSym[j];
        final int index = j;
        if (index == 2) {
          state = s1;
        }
        if (index > 2) {
          ITransition tb = new FilteredTransition(state, s0, v,
              new ISymbol[] { v }, new FilteredTransition.IFilter() {
                public List invoke(ISymbol symbol, List outputs) {
                  return generateNewOutputs(symbol, charSym, index);
                }
              }, new FilteredTransition.ICondition() {
                public boolean accept(ISymbol symbol, IMatchContext ctx) {
                  boolean r = !symbol.equals(finalChar);
                  if (r)
                    System.err.println("(in filtered transition: accept "
                        + symbol + ")");
                  return r;
                }
              });
          transitions.add(tb);
        }
        if (index < charSym.length - 1) {
          nextState = new State("s" + (i + 1));
          ITransition ta = new FilteredTransition(state, nextState, v,
              new ISymbol[] {}, null, new FilteredTransition.ICondition() {
                public boolean accept(ISymbol symbol, IMatchContext ctx) {
                  boolean r = symbol.equals(finalChar);
                  if (r)
                    System.err.println("(in filtered transition: accept "
                        + symbol + ")");
                  return r;
                }
              });
          transitions.add(ta);
          state = nextState;
          finalStates.add(state);
          i++;
        }
        if (index == charSym.length - 1) {
          ITransition ta = new FilteredTransition(state, s0, v,
              new ISymbol[] { charSym[0] }, null,
              new FilteredTransition.ICondition() {
                public boolean accept(ISymbol symbol, IMatchContext ctx) {
                  boolean r = symbol.equals(finalChar);
                  if (r)
                    System.err.println("(in filtered transition: accept "
                        + symbol + ")");
                  return r;
                }
              });
          transitions.add(ta);
        }
      }
    }
    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

  private CharSymbol[] getSecondChars(List charSymbolArrayList) {
    CharSymbol[] secondChars = new CharSymbol[charSymbolArrayList.size()];
    Iterator charSymIte = charSymbolArrayList.iterator();
    int index = 0;
    while (charSymIte.hasNext()) {
      CharSymbol[] sym = (CharSymbol[]) charSymIte.next();
      secondChars[index] = sym[2];
      index++;
    }
    return secondChars;
  }

  protected List generateNewOutputs(ISymbol symbol, CharSymbol[] charSym,
                                    int index) {
    List newOutputs = new ArrayList();
    for (int i = 1; i < index; i++) {
      newOutputs.add(charSym[i]);
    }
    newOutputs.add(symbol);
    return newOutputs;
  }

  protected boolean containsChar(ISymbol symbol, CharSymbol[] chars) {
    for (int i = 0; i < chars.length; i++) {
      if (symbol.equals(chars[i])) {
        return true;
      }
    }
    return false;
  }
}