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

import java.util.*;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.stringAnalysis.translator.*;

public abstract class TranslatorRepository implements ITranslatorRepository {
  static class UnsupportedTranslator implements ITranslator {
    public ITranslator copy() {
      return this;
    }


    public SimpleGrammar solve(IConstraintSolver solver, String funcName, ISymbol recv, List params, IProductionRule invokeRule, SimpleGrammar targetGrammar, SimpleGrammar otherRules, Stack<CallEnv> callStack, IVariableFactory varFactory) {
      throw (new RuntimeException("not implemented yet: " + funcName));
    }

    public SimpleGrammar translateCyclic(SimpleGrammar cfg, Set terminals) {
      throw (new RuntimeException("not implemented yet"));
    }

    public Set possibleOutputs(Set terminals) {
      throw (new RuntimeException("not implemented yet"));
    }

    public boolean acceptCyclic() {
      return false;
    }

    public boolean isFixpoint(SimpleGrammar target,
                              SimpleGrammar translatedGrammar) {
      throw (new RuntimeException("not implemented yet"));
    }

    public SimpleGrammar toComparable(SimpleGrammar target) {
      throw (new RuntimeException("not implemented yet"));
    }
  };

  static protected ITranslator UNSUPPORTED_TRANSLATOR = new UnsupportedTranslator();

  protected final Map<String,ITranslator> translatorMap = new HashMap<String,ITranslator>();
  protected ITranslatorRepository prevRepository = null;

  public TranslatorRepository(ITranslatorRepository prevRepository) {
    this.prevRepository = prevRepository;
  }

  public TranslatorRepository() {
    this.prevRepository = null;
  }

  public ITranslator getTranslator(String funcName) {
    if (translatorMap.containsKey(funcName)) {
      return ((ITranslator) translatorMap.get(funcName)).copy();
    }
    else if (prevRepository != null) {
      return prevRepository.getTranslator(funcName);
    }
    else {
      throw (new RuntimeException("no such translator: " + funcName));
    }
  }
}