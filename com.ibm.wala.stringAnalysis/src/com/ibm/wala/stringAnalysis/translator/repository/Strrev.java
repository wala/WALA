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
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.stringAnalysis.translator.IConstraintSolver;

public class Strrev extends StringTranslator {
  public Strrev() {
    super(TARGET_ALL);
  }

  public SimpleGrammar prepare(IConstraintSolver translator, String funcName,
                               ISymbol recv, List params, IProductionRule rule,
                               SimpleGrammar g, IVariableFactory varFactory) {
    SimpleGrammar g2 = super.prepare(translator, funcName, recv, params, rule,
      g, varFactory);
    Variable target = (Variable) params.get(0);
    Set rules = (Set) g2.getRules();
    Iterator rulesIte = rules.iterator();
    List targetList = new ArrayList();
    while (rulesIte.hasNext()) {
      ProductionRule varRule = (ProductionRule) rulesIte.next();
      if (varRule.getLeft().equals(target)) {
        targetList = varRule.getRight();
        g2.getRules().remove(varRule);
        break;
      }
    }
    List revTargetList = new ArrayList();
    for (int i = targetList.size() - 1; i >= 0; i--) {
      revTargetList.add(targetList.get(i));
    }
    g2.getRules().add(new ProductionRule(target, revTargetList));
    g2.setStartSymbol(target);
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