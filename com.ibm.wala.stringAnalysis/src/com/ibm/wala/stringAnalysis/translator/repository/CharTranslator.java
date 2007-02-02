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
import java.util.List;
import java.util.Set;

import com.ibm.wala.automaton.string.CharSymbol;
import com.ibm.wala.automaton.string.IMatchContext;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.StringSymbol;
import com.ibm.wala.stringAnalysis.translator.repository.Homomorphism.Rule;

public abstract class CharTranslator extends Homomorphism {
  public CharTranslator(int n) {
    super(n);
  }

  public CharTranslator() {
    super();
  }

  public Set<Rule> getRuleSet() {
    Rule r = new Rule() {
      public boolean accept(ISymbol symbol, IMatchContext ctx) {
        assert (symbol instanceof CharSymbol);
        return true;
      }

      public List invoke(ISymbol symbol, List outputs) {
        assert (symbol instanceof CharSymbol);
        CharSymbol csym = (CharSymbol) symbol;
        char cs[] = translate(csym.charValue());
        String s = new String(cs);
        return StringSymbol.toCharSymbols(s);
      }
    };
    Set<Rule> rs = new HashSet<Rule>();
    rs.add(r);
    return rs;
  }

  protected abstract char[] translate(char c);
}