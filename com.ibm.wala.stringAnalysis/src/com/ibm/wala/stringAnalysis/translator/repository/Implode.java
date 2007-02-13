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
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.IVariableFactory;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.stringAnalysis.translator.IConstraintSolver;

public class Implode extends StringTranslator {
//  private IntRange ranges[];

  private int glueVarIndex;

  private int piecesVarIndex;

  public Implode(int glue, int pieces) {
    super(pieces);
    this.piecesVarIndex = pieces;
    this.glueVarIndex = glue;
  }

  public SimpleGrammar translate(SimpleGrammar cfg) {
    return cfg;
  }

  public SimpleGrammar prepare(IConstraintSolver translator, String funcName,
                               ISymbol recv, List params, IProductionRule rule,
                               SimpleGrammar g, IVariableFactory varFactory) {
    SimpleGrammar g2 = super.prepare(translator, funcName, recv, params, rule,
      g, varFactory);

    Variable piecesVar = (Variable) params.get(piecesVarIndex);
    Variable glueVar = (Variable) params.get(glueVarIndex);

    Set rules = (Set) g2.getRules();
    Iterator rulesIte = rules.iterator();
    List pieces = new ArrayList();
    List ruleList = new ArrayList();
    while (rulesIte.hasNext()) {
      ProductionRule g2Rule = (ProductionRule) rulesIte.next();
      IVariable left = g2Rule.getLeft();
      List right = g2Rule.getRight();
      if (left.equals(piecesVar)) {
        pieces.add(right.get(0));
        ruleList.add(g2Rule);
      }
    }

    List l = new ArrayList();
    Iterator piecesIte = pieces.iterator();
    while (piecesIte.hasNext()) {
      ISymbol s = (ISymbol) piecesIte.next();
      l.add(s);
      l.add(glueVar);
    }
    l.remove(l.size() - 1);
    
    IProductionRule newRule = new ProductionRule(g2.getStartSymbol(), l);
    Iterator ruleListIte = ruleList.iterator();
    while (ruleListIte.hasNext()) {
      g2.getRules().remove(ruleListIte.next());
    }
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