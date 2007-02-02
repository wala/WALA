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
package com.ibm.wala.automaton.string;

import java.util.Collection;

public class DeepTransitionCopier extends AbstractTransitionCopier implements ITransitionCopier {
  private ISymbolCopier symbolCopier;
  private IStateCopier stateCopier;

  public DeepTransitionCopier(ISymbolCopier symbolCopier, IStateCopier stateCopier) {
    this.symbolCopier = symbolCopier;
    this.stateCopier = stateCopier;
  }

  public DeepTransitionCopier() {
    this(SimpleSymbolCopier.defaultCopier, SimpleStateCopier.defaultCopier);
  }

  public ITransition copy(ITransition transition) {
    return (ITransition) transition.clone();
  }

  public IState copy(IState state) {
    return state.copy(stateCopier);
  }

  public Collection copyStates(Collection c) {
    return stateCopier.copyStates(c);
  }

  public String copyName(String name) {
    return symbolCopier.copyName(name);
  }

  public String copyStateName(String name) {
    return stateCopier.copyStateName(name);
  }

  public IState copyStateReference(IState parent, IState state) {
    return stateCopier.copyStateReference(parent, state);
  }

  public Collection copyStateReferences(IState parent, Collection c) {
    return stateCopier.copyStateReferences(parent, c);
  }

  public ISymbol copy(ISymbol symbol) {
    return symbol.copy(symbolCopier);
  }

  public Collection copySymbols(Collection symbols) {
    return symbolCopier.copySymbols(symbols);
  }

  public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol) {
    return symbolCopier.copySymbolReference(parent, symbol);
  }

  public Collection copySymbolReferences(ISymbol parent, Collection c) {
    return symbolCopier.copySymbolReferences(parent, c);
  }

}
