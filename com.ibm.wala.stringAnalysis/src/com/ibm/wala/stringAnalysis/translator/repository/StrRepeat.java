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
import java.util.List;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleGrammar;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.IVariableFactory;
import com.ibm.wala.automaton.string.NumberSymbol;
import com.ibm.wala.stringAnalysis.translator.IConstraintSolver;

public class StrRepeat extends StringTranslator {
  private int repNumIndex;

  public StrRepeat(int target, int rep) {
    super(target);
    this.repNumIndex = rep;
  }

  public StrRepeat(int rep) {
    super();
    this.repNumIndex = rep;
  }

  public SimpleGrammar prepare(IConstraintSolver translator, String funcName,
                               ISymbol recv, List params,
                               IProductionRule rule, SimpleGrammar g,
                               IVariableFactory varFactory) {
    NumberSymbol repNumSym = (NumberSymbol) params.get(repNumIndex);
    int repNum = repNumSym.intValue();

    SimpleGrammar g2 = super.prepare(translator, funcName, recv, params, rule, g, varFactory);
    List<ISymbol> l = new ArrayList<ISymbol>();
    for (int i = 0; i < repNum; i++) {
      l.add(g2.getStartSymbol());
    }

    IVariable v = createVariable();
    IProductionRule newRule = new ProductionRule(v, l);
    g2.getRules().add(newRule);
    g2.setStartSymbol(v);
    return g2;
  }

  public boolean acceptCyclic() {
    return true;
  }

  public Set possibleOutputs(Set terminals) {
    return terminals;
  }

  public SimpleGrammar translate(SimpleGrammar cfg) {
    return cfg;
  }

  public SimpleGrammar translateCyclic(SimpleGrammar cfg, Set terminals) {
    return translate(cfg);
  }

}