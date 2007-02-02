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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleGrammar;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariableFactory;
import com.ibm.wala.stringAnalysis.translator.IConstraintSolver;

public class Concatenate extends StringTranslator {
  private IntRange ranges[];

  public Concatenate(IntRange ranges[]) {
    super(TARGET_ALL);
    this.ranges = ranges;
  }

  public Concatenate() {
    this(new IntRange[] { new IntRange(0, -1) });
  }

  public SimpleGrammar translate(SimpleGrammar cfg) {
    return cfg;
  }

  public SimpleGrammar prepare(IConstraintSolver translator, String funcName,
                               ISymbol recv, List params,
                               IProductionRule rule, SimpleGrammar g,
                               IVariableFactory varFactory) {
    SimpleGrammar g2 = super.prepare(translator, funcName, recv, params,
      rule, g, varFactory);
    List l = new ArrayList();
    for (int i = 0; i < ranges.length; i++) {
      for (Iterator r = ranges[i].iteratorFor(params); r.hasNext();) {
        ISymbol s = (ISymbol) r.next();
        l.add(s);
      }
    }
    IProductionRule newRule = new ProductionRule(rule.getLeft(), l);
    g2.getRules().remove(rule);
    g2.getRules().add(newRule);
    return g2;
  }

  public SimpleGrammar translateCyclic(SimpleGrammar cfg, Set terminals) {
    return translate(cfg);
  }

  public Set possibleOutputs(Set terminals) {
    return terminals;
  }

  public boolean acceptCyclic() {
    return true;
  }
}