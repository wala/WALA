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

import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.IVariableFactory;

public class TreeVariableFactory implements IVariableFactory<ITreeVariable> {
  private IVariableFactory baseFactory;
  
  public TreeVariableFactory(IVariableFactory baseFactory) {
    this.baseFactory = baseFactory;
  }

  public ITreeVariable createVariable(String name) {
    return createSymbol(name);
  }

  public ITreeVariable createSymbol(String name) {
    return new TreeVariable(baseFactory.createVariable(name));
  }
}
