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
package com.ibm.wala.stringAnalysis.translator.repository;

import java.text.DecimalFormat;
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

public class Addcslashes extends Transducer {

  private int charlistVarIndex;

  private CharSymbol[] charSyms;

  private final CharSymbol backslashe = new CharSymbol("\\");

  private final CharSymbol[] octChars = (CharSymbol[]) new StringSymbol(
      "01234567").toCharSymbols().toArray(new CharSymbol[0]);

  public Addcslashes(int target, int charlist) {
    super(target);
    this.charlistVarIndex = charlist;
  }

  public Addcslashes(int charlist) {
    this.charlistVarIndex = charlist;
  }

  public IAutomaton getTransducer() {
    Variable v = new Variable("v");

    Set<ITransition> transitions = new HashSet<ITransition>();
    IState s0 = new State("s0");
    IState s1 = new State("s1");
    IState initState = s0;
    Set<IState> finalStates = new HashSet<IState>();
    finalStates.add(s0);
    finalStates.add(s1);

    charSyms = getCharSymbols();
    ITransition t0 = new FilteredTransition(s0, s0, v, new ISymbol[] {
      backslashe, v }, new FilteredTransition.IFilter() {
      public List invoke(ISymbol symbol, List outputs) {
        CharSymbol charSym = (CharSymbol) symbol;
        int charVal = charSym.charValue();
        if (126 < charVal || charVal < 32) {
          DecimalFormat df = new DecimalFormat("000");
          List octalStringSym = new StringSymbol(df.format(Double
              .parseDouble(Integer.toOctalString(charVal)))).toCharSymbols();
          octalStringSym.add(0, backslashe);
          return octalStringSym;
        }
        return outputs;
      }
    }, new FilteredTransition.ICondition() {
      public boolean accept(ISymbol symbol, IMatchContext ctx) {
        boolean r = containsCharSymbols(symbol, charSyms);
        if (r)
          System.err.println("(containsCharSymbols(symbol, charSyms) " + symbol
              + ")");
        return r;
      }
    });
    transitions.add(t0);
    ITransition t1 = new FilteredTransition(s0, s0, v, new ISymbol[] { v },
        null, new FilteredTransition.ICondition() {
          public boolean accept(ISymbol symbol, IMatchContext ctx) {
            boolean r = !containsCharSymbols(symbol, charSyms);
            if (r)
              System.err.println("(!containsCharSymbols(symbol, charSyms) "
                  + symbol + ")");
            return r;
          }
        });
    transitions.add(t1);

    Automaton transducer = new Automaton(initState, finalStates, transitions);
    return transducer;
  }

  protected boolean containsCharSymbols(ISymbol symbol, CharSymbol[] charSyms) {
    for (int i = 0; i < charSyms.length; i++) {
      if (symbol.equals(charSyms[i])) {
        return true;
      }
    }
    return false;
  }

  private CharSymbol[] getCharSymbols() {
    Variable charlistVar = (Variable) params.get(charlistVarIndex);
    List<CharSymbol> charlist = new ArrayList<CharSymbol>();
    Set rules = (Set) grammar.getRules();
    Iterator rulesIte = rules.iterator();
    while (rulesIte.hasNext()) {
      ProductionRule rule = (ProductionRule) rulesIte.next();
      if (rule.getLeft().equals(charlistVar)) {
        charlist = rule.getRight();
        break;
      }
    }
    String str = list2Str(charlist);
    int doubleDotIndex = str.indexOf("..");
    if (doubleDotIndex > 0) {
      List<CharSymbol> charlist2 = new ArrayList<CharSymbol>();
      do {
        for (int i = 0; i < doubleDotIndex - 1; i++) {
          charlist2.add((CharSymbol) charlist.get(i));
        }
        char startChar = str.charAt(doubleDotIndex - 1);
        char endChar = str.charAt(doubleDotIndex + 2);
        if (startChar > endChar) {
          charlist2.add(new CharSymbol((char) startChar));
          charlist2.add(new CharSymbol("."));
        }
        else {
          int charSymsLen = endChar - startChar + 1;
          for (int i = 0; i < charSymsLen; i++) {
            charlist2.add(new CharSymbol((char) (startChar + i)));
          }
          if ((startChar <= 'Z') && (endChar >= 'a')) {
            charlist2.add(new CharSymbol("\t"));
            charlist2.add(new CharSymbol("\n"));
            charlist2.add(new CharSymbol("\r"));
          }
        }
        doubleDotIndex += 3;
        if (str.length() > doubleDotIndex) {
          str = str.substring(doubleDotIndex);
          charlist = charlist.subList(doubleDotIndex, charlist.size());
          doubleDotIndex = str.indexOf("..");
        }
        else {
          break;
        }
      } while (doubleDotIndex > 0);
      charlist.addAll(charlist2);
    }
    CharSymbol[] charSyms = (CharSymbol[]) charlist.toArray(new CharSymbol[0]);
    return charSyms;
  }

  private String list2Str(List charlist) {
    StringBuffer sb = new StringBuffer();
    Iterator charlistIte = charlist.iterator();
    while (charlistIte.hasNext()) {
      CharSymbol chr = (CharSymbol) charlistIte.next();
      sb.append(chr.charValue());
    }
    return sb.toString();
  }

}
