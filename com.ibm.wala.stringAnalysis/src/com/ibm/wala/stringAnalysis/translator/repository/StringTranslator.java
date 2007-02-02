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

import com.ibm.wala.automaton.grammar.string.CFLReachability;
import com.ibm.wala.automaton.grammar.string.ContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.Grammars;
import com.ibm.wala.automaton.grammar.string.IContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.SimpleGrammar;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IVariable;

public abstract class StringTranslator extends BasicTranslator {
  public boolean isFixpoint(SimpleGrammar target,
                            SimpleGrammar translatedGrammar) {
    IContextFreeGrammar cfg = new ContextFreeGrammar(target);
    IAutomaton fst = Grammars.toAutomaton(cfg);
    return CFLReachability.containsAll(fst, new ContextFreeGrammar(
        translatedGrammar));
  }
  
  public StringTranslator() {
    super();
  }
  
  public StringTranslator(int target) {
    super(target);
  }
  

  public SimpleGrammar toComparable(SimpleGrammar target) {
    IContextFreeGrammar cfg = new ContextFreeGrammar(target);
    Grammars.RegularApproximation.approximateToRegular(cfg);
    return cfg.toSimple();
  }

  protected IVariable createVariable() {
    return varFactory.createVariable(Grammars.variablePrefix);
  }
}