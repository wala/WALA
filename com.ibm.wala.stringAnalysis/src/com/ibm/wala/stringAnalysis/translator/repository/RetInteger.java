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
import com.ibm.wala.stringAnalysis.translator.IConstraintSolver;

public class RetInteger extends StringTranslator {

  public SimpleGrammar prepare(IConstraintSolver translator, String funcName,
                               ISymbol recv, List params,
                               IProductionRule rule, SimpleGrammar g,
                               IVariableFactory varFactory) {
    SimpleGrammar g2 = super.prepare(translator, funcName, recv, params,
      rule, g, varFactory);
    IVariable S = g2.getStartSymbol();
    IVariable N = varFactory.createVariable("N");
    ContextFreeGrammar g3 = new ContextFreeGrammar(S, new IProductionRule[] {
      new ProductionRule(S, new ISymbol[] { N }),
      new ProductionRule(S, new ISymbol[] { N, S }),
      new ProductionRule(N, new ISymbol[] { new CharSymbol("0") }),
      new ProductionRule(N, new ISymbol[] { new CharSymbol("1") }),
      new ProductionRule(N, new ISymbol[] { new CharSymbol("2") }),
      new ProductionRule(N, new ISymbol[] { new CharSymbol("3") }),
      new ProductionRule(N, new ISymbol[] { new CharSymbol("4") }),
      new ProductionRule(N, new ISymbol[] { new CharSymbol("5") }),
      new ProductionRule(N, new ISymbol[] { new CharSymbol("6") }),
      new ProductionRule(N, new ISymbol[] { new CharSymbol("7") }),
      new ProductionRule(N, new ISymbol[] { new CharSymbol("8") }),
      new ProductionRule(N, new ISymbol[] { new CharSymbol("9") }), });
    return g3;
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