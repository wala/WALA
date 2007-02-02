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

import java.util.Set;

public class FreshSymbolFactory<T extends ISymbol> extends FreshNameFactory implements ISymbolFactory<T> {
  private ISymbolFactory<T> baseFactory;
  
  public FreshSymbolFactory(ISymbolFactory<T> factory, Set<String> names) {
    super(names);
    this.baseFactory = factory;
  }
  
  public T createSymbol(String name) {
    return baseFactory.createSymbol(super.createName(name));
  }
}
