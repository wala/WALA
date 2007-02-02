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
import com.ibm.wala.automaton.string.NumberSymbol;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.stringAnalysis.translator.IConstraintSolver;

public class Numberformat extends StringTranslator {

  private int decimalsNumIndex;

  private int decPointVarIndex;

  private int sepVarIndex;

  public Numberformat(int decimals, int dec, int sep) {
    this.decimalsNumIndex = decimals;
    this.decPointVarIndex = dec;
    this.sepVarIndex = sep;
  }

  public SimpleGrammar prepare(IConstraintSolver translator, String funcName,
                               ISymbol recv, List params, IProductionRule rule,
                               SimpleGrammar g, IVariableFactory varFactory) {

    SimpleGrammar g2 = super.prepare(translator, funcName, recv, params, rule,
      g, varFactory);

    CharSymbol decPoint = new CharSymbol("");
    CharSymbol sep = new CharSymbol(",");
    int decimal = 0;
    
    if (params.size() > decimalsNumIndex) {
      NumberSymbol decimalNum = (NumberSymbol) params.get(decimalsNumIndex);
      decimal = decimalNum.intValue();
      decPoint = new CharSymbol(".");
    }
    if (params.size() > decPointVarIndex) {
      Variable decPointVar = (Variable) params.get(decPointVarIndex);
      Variable sepVar = (Variable) params.get(sepVarIndex);

      Set rules = (Set) g2.getRules();
      Iterator rulesIte = rules.iterator();
      while (rulesIte.hasNext()) {
        ProductionRule varRule = (ProductionRule) rulesIte.next();
        IVariable left = varRule.getLeft();
        List right = varRule.getRight();
        if (left.equals(decPointVar)) {
          decPoint = (CharSymbol) right.get(0);
        }
        else if (left.equals(sepVar)) {
          sep = (CharSymbol) right.get(0);
        }
      }
    }

    IVariable G0 = g2.getStartSymbol();
    IVariable G1 = varFactory.createVariable("G1");
    IVariable G2 = varFactory.createVariable("G2");
    IVariable N = varFactory.createVariable("N");
    ISymbol[] syms = new ISymbol[] { N, G1, N };
    if (params.size() > decimalsNumIndex && decimal != 0) {
      syms = new ISymbol[] { N, G1, decPoint, G2 };
    }
    ContextFreeGrammar g3 = new ContextFreeGrammar(G0, new IProductionRule[] {
      // new ProductionRule(G0, new ISymbol[] { N, G1, N}),
      new ProductionRule(G0, syms),
      new ProductionRule(G1, new ISymbol[] { N }),
      new ProductionRule(G1, new ISymbol[] { sep }),
      new ProductionRule(G1, new ISymbol[] { G1, G1 }),
      new ProductionRule(G2, new ISymbol[] { N }),
      new ProductionRule(G2, new ISymbol[] { N, G2 }),
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

  public SimpleGrammar translate(SimpleGrammar g) {
    return g;
  }

  public SimpleGrammar translateCyclic(SimpleGrammar g, Set terminals) {
    return translate(g);
  }
}
