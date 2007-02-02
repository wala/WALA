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
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.IVariableFactory;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.stringAnalysis.translator.IConstraintSolver;

public class Strtok extends StringTranslator {

  public SimpleGrammar prepare(IConstraintSolver translator, String funcName,
                               ISymbol recv, List params, IProductionRule rule,
                               SimpleGrammar g, IVariableFactory varFactory) {
    SimpleGrammar g2 = super.prepare(translator, funcName, recv, params, rule,
      g, varFactory);

    List tokenList = getTokenList();

    IVariable S = g2.getStartSymbol();
    IVariable G = varFactory.createVariable("G");
    List productionRuleList = new ArrayList();

    productionRuleList.add(new ProductionRule(S, new ISymbol[] { G }));
    productionRuleList.add(new ProductionRule(S, new ISymbol[] { G, S }));
    for (int i = 0; i < 256; i++) {
      CharSymbol chr = new CharSymbol((char) i);
      if (!tokenList.contains(chr)) {
        productionRuleList.add(new ProductionRule(G, new ISymbol[] { chr }));
      }
    }
    IProductionRule[] productionRules = (ProductionRule[]) productionRuleList
        .toArray(new ProductionRule[0]);
    ContextFreeGrammar g3 = new ContextFreeGrammar(S, productionRules);
    return g3;
  }

  private List getTokenList() {
    Variable tokenVar = (Variable) params.get(params.size() - 1);
    Set rules = (Set) grammar.getRules();
    Iterator rulesIte = rules.iterator();
    List tokenList = new ArrayList();
    while (rulesIte.hasNext()) {
      ProductionRule rule = (ProductionRule) rulesIte.next();
      if (rule.getLeft().equals(tokenVar)) {
        tokenList = rule.getRight();
        break;
      }
    }
    return tokenList;
  }

  public SimpleGrammar translate(SimpleGrammar g) {
    return g;
  }

  public boolean acceptCyclic() {
    return true;
  }

  public Set possibleOutputs(Set terminals) {
    return terminals;
  }

  public SimpleGrammar translateCyclic(SimpleGrammar g, Set terminals) {
    return translate(g);
  }
}
