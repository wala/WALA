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

import com.ibm.wala.automaton.grammar.string.Grammars;
import com.ibm.wala.automaton.grammar.string.IGrammar;

public class FreshVariableFactory extends FreshSymbolFactory<IVariable> implements IVariableFactory<IVariable> {
  public FreshVariableFactory(ISymbolFactory<IVariable> factory, Set<String> usedVarNames) {
    super(factory, usedVarNames);
  }
  
  public FreshVariableFactory(ISymbolFactory<IVariable> factory, IGrammar g) {
    this(factory, Grammars.collectVariableNames(Grammars.collectVariables(g)));
  }
  
  public IVariable createSymbol(String name) {
    return super.createSymbol(name);
  }
  
  public IVariable createVariable(String name) {
    return createSymbol(name);
  }
}
