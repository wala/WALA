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

import com.ibm.wala.automaton.grammar.string.ContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleGrammar;
import com.ibm.wala.automaton.string.CharSymbol;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariableFactory;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.stringAnalysis.translator.IConstraintSolver;

public class Strshuffle extends StringTranslator {

  private int target;

  public Strshuffle(int target) {
    this.target = target;
  }

  public Strshuffle() {
    this.target = -1;
  }

  public SimpleGrammar prepare(IConstraintSolver translator, String funcName,
                               ISymbol recv, List params,
                               IProductionRule rule, SimpleGrammar cfg,
                               IVariableFactory varFactory) {
    super.prepare(translator, funcName, recv, params, rule, cfg, varFactory);

    ISymbol sym = (target >= 0) ? (ISymbol) params.get(target) : recv;

    Set rules = (Set) cfg.getRules();
    Iterator rulesIte = rules.iterator();
    List targetList = new ArrayList();
    while (rulesIte.hasNext()) {
      ProductionRule varRule = (ProductionRule) rulesIte.next();
      if (varRule.getLeft().equals(sym)) {
        targetList = varRule.getRight();
        cfg.getRules().remove(varRule);
        break;
      }
    }
    IProductionRule[] newRules = new IProductionRule[targetList.size() + 2];
    newRules[0] = new ProductionRule(new Variable("G"),
        new ISymbol[] { new Variable("A") });
    newRules[1] = new ProductionRule(new Variable("G"), new ISymbol[] {
      new Variable("A"), new Variable("G") });
    Iterator targetListIte = targetList.iterator();
    int index = 2;
    while (targetListIte.hasNext()) {
      CharSymbol chr = (CharSymbol) targetListIte.next();
      newRules[index] = new ProductionRule(new Variable("A"),
          new ISymbol[] { chr });
      index++;
    }
    ContextFreeGrammar newCfg = new ContextFreeGrammar(new Variable("G"),
        newRules);
    return newCfg;
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