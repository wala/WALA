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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.CFLTranslator;
import com.ibm.wala.automaton.grammar.string.ContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.Grammars;
import com.ibm.wala.automaton.grammar.string.IContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleGrammar;
import com.ibm.wala.automaton.string.Automatons;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.ITransitionVisitor;
import com.ibm.wala.automaton.string.IVariableFactory;
import com.ibm.wala.stringAnalysis.translator.IConstraintSolver;
import com.ibm.wala.stringAnalysis.util.SAUtil;

public abstract class Transducer extends StringTranslator {
  private IAutomaton transducer;

  public Transducer(int target) {
    super(target);
  }

  public Transducer() {
    super();
  }

  public SimpleGrammar prepare(IConstraintSolver translator, String funcName,
                               ISymbol recv, List params,
                               IProductionRule rule, SimpleGrammar g,
                               IVariableFactory varFactory) {
    SimpleGrammar g2 = super.prepare(translator, funcName, recv, params, rule, g, varFactory);
    transducer = getTransducer();
    return g2;
  }

  public abstract IAutomaton getTransducer();

  public SimpleGrammar translate(SimpleGrammar g) {
    IContextFreeGrammar g2 = CFLTranslator.translate(transducer,
      new ContextFreeGrammar(g));
    return g2.toSimple();
  }

  public boolean acceptCyclic() {
    return false;
  }

  public SimpleGrammar translateCyclic(SimpleGrammar g, Set terminals) {
    System.err.println("Warning: cyclic constraint: " + funcName);
    IContextFreeGrammar g2 = Grammars.toCFG(transducer, terminals,
      Grammars.TransitionOutput.defaultInstance);
    Grammars.useUniqueVariables(g2, varFactory, new HashMap());
    return g2.toSimple();
  }

  public Set possibleOutputs(Set terminals) {
    IAutomaton fst = Automatons.expand(transducer, terminals);
    final Set outputs = new HashSet();
    fst.traverseTransitions(new ITransitionVisitor() {
      public void onVisit(ITransition transition) {
        outputs.addAll(SAUtil.set(transition.getOutputSymbols()));
      }
    });
    return outputs;
  }
}