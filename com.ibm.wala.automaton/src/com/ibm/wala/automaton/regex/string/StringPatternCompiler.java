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
package com.ibm.wala.automaton.regex.string;

import java.util.*;

import com.ibm.wala.automaton.string.*;

public class StringPatternCompiler extends AbstractPatternCompiler {
  static public final Set<CharSymbol> allChars = new HashSet<CharSymbol>();
  static {
    for (char c = 0; c < 255; c++) {
      allChars.add(new CharSymbol(c));
    }
  }
  
  public IAutomaton onComplement(ComplementPattern pattern) {
    return Automatons.createComplement(compile(pattern.getPattern()), allChars);
  }

  public IAutomaton onConcatenation(ConcatenationPattern pattern) {
    return Automatons.createConcatenation(
      compile(pattern.getHead()),
      compile(pattern.getTail()));
  }

  public IAutomaton onEmpty(EmptyPattern pattern) {
    return Automatons.createAutomaton(new ISymbol[0]);
  }

  public IAutomaton onIntersection(IntersectionPattern pattern) {
    return Automatons.createIntersection(
      compile(pattern.getLeft()),
      compile(pattern.getRight()));
  }

  public IAutomaton onIteration(IterationPattern pattern) {
    IAutomaton a = compile(pattern.getPattern());
    IState initState = a.getInitialState();
    for (Iterator i = a.getFinalStates().iterator(); i.hasNext(); ) {
      IState finState = (IState) i.next();
      ITransition t = new Transition(finState, initState, Transition.EpsilonSymbol);
      a.getTransitions().add(t);
    }
    if (pattern.includesEmpty()) {
      a.getFinalStates().clear();
      a.getFinalStates().add(initState);
    }
    return a;
  }

  public IAutomaton onSymbol(SymbolPattern pattern) {
    ISymbol s = pattern.getSymbol();
    if ((s instanceof CharPatternSymbol) && s.getName().equals("\\.")) {
      return Automatons.createAutomaton(new ISymbol[]{new Variable(".")});
    }
    else {
      return Automatons.createAutomaton(new ISymbol[]{s});
    }
  }

  public IAutomaton onUnion(UnionPattern pattern) {
    return Automatons.createUnion(
      compile(pattern.getLeft()),
      compile(pattern.getRight()));
  }

  public IAutomaton onVariableBinding(VariableBindingPattern pattern) {
    // TODO: implement this method
    return compile(pattern.getPattern());
  }

  public IAutomaton onVariableReference(VariableReferencePattern pattern) {
    // TODO: implement this method
    return null;
  }

}
