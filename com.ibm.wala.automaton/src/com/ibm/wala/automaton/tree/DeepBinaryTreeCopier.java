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
package com.ibm.wala.automaton.tree;

import java.util.Collection;

import com.ibm.wala.automaton.string.*;

public class DeepBinaryTreeCopier implements IBinaryTreeCopier {
  ISymbolCopier baseCopier;
  
  public DeepBinaryTreeCopier(ISymbolCopier copier) {
    this.baseCopier = copier;
  }

  public ISymbol copyLabel(ISymbol parent, ISymbol label) {
    return copySymbolReference(parent, label);
  }

  public ISymbol copy(ISymbol symbol) {
    return baseCopier.copy(symbol);
  }

  public String copyName(String name) {
    return baseCopier.copyName(name);
  }

  public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol) {
    return baseCopier.copySymbolReference(parent, symbol);
  }

  public Collection copySymbolReferences(ISymbol parent, Collection symbols) {
    return baseCopier.copySymbolReferences(parent, symbols);
  }

  public Collection copySymbols(Collection symbols) {
    return baseCopier.copySymbols(symbols);
  }
}
