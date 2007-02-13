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
package com.ibm.wala.stringAnalysis.test.translator;

import com.ibm.wala.automaton.grammar.string.ContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.Grammars;
import com.ibm.wala.automaton.grammar.string.IContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleGrammar;
import com.ibm.wala.automaton.regex.string.IPattern;
import com.ibm.wala.automaton.regex.string.SymbolPattern;
import com.ibm.wala.automaton.regex.string.UnionPattern;
import com.ibm.wala.automaton.string.CharSymbol;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.stringAnalysis.test.SAJunitBase;
import com.ibm.wala.stringAnalysis.translator.RuleAdder;
import com.ibm.wala.stringAnalysis.translator.VariableDefiner;

public class TestTranslators extends SAJunitBase {
  public void testRuleAdder1() {
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
      }
    );
    ContextFreeGrammar expected = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new CharSymbol("a")}),
      }
    );
    RuleAdder adder = new RuleAdder(cfg1,
      new IProductionRule[]{
      new ProductionRule(new Variable("B"), new ISymbol[]{new CharSymbol("a")}),
      }
    );
    SimpleGrammar sg = adder.toSimple();
    assertEquals(expected, sg);
  }
  
  public void testVariableDefiner1() {
    ContextFreeGrammar cfg1 = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
      }
    );
    ContextFreeGrammar expected = new ContextFreeGrammar(
      new Variable("A"),
      new IProductionRule[]{
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B"), new Variable("A")}),
        new ProductionRule(new Variable("A"), new ISymbol[]{}),
        new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new CharSymbol("a")}),
        new ProductionRule(new Variable("B"), new ISymbol[]{new CharSymbol("b")}),
      }
    );
    VariableDefiner definer = new VariableDefiner(cfg1, new IVariable[]{new Variable("B")},
      new IPattern[]{
        new UnionPattern(
          new SymbolPattern(new CharSymbol("a")),
          new SymbolPattern(new CharSymbol("b")))
      }
    );
    IContextFreeGrammar cfg2 = (IContextFreeGrammar) definer.toSimple();
    Grammars.eliminateEpsilonRules(cfg2);
    Grammars.eliminateUselessRules(cfg2);
    Grammars.eliminateDanglingVariables(cfg2);
    assertEquals(expected, cfg2);
  }
}
