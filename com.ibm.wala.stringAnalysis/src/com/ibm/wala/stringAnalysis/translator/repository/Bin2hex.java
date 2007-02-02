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

public class Bin2hex extends StringTranslator {

  @SuppressWarnings("unchecked")
  public SimpleGrammar prepare(IConstraintSolver translator, String funcName,
                               ISymbol recv, List params,
                               IProductionRule rule, SimpleGrammar cfg,
                               IVariableFactory varFactory) {
    super.prepare(translator, funcName, recv, params, rule, cfg, varFactory);
    IVariable S = cfg.getStartSymbol();
    IVariable AN = varFactory.createVariable("AN");
    ContextFreeGrammar newCfg = new ContextFreeGrammar(S,
        new IProductionRule[] { new ProductionRule(S, new ISymbol[] { AN }),
          new ProductionRule(S, new ISymbol[] { AN, S }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("0") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("1") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("2") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("3") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("4") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("5") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("6") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("7") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("8") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("9") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("a") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("b") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("c") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("d") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("e") }),
          new ProductionRule(AN, new ISymbol[] { new CharSymbol("f") }) });
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