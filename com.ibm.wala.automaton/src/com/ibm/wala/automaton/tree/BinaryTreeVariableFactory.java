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

import com.ibm.wala.automaton.string.IVariableFactory;

public class BinaryTreeVariableFactory implements IVariableFactory<IBinaryTreeVariable> {
  private IVariableFactory baseFactory;
  
  public BinaryTreeVariableFactory(IVariableFactory baseFactory) {
    this.baseFactory = baseFactory;
  }

  public IBinaryTreeVariable createVariable(String name) {
    return createSymbol(name);
  }

  public IBinaryTreeVariable createSymbol(String name) {
    return new BinaryTreeVariable(baseFactory.createVariable(name));
  }
}
