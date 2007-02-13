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


public class SimpleVariableFactory implements IVariableFactory<IVariable> {
  public IVariable createSymbol(String name) {
    return new Variable(name);
  }

  public IVariable createVariable(String name) {
    return createSymbol(name);
  }
  
  static public SimpleVariableFactory defaultFactory = new SimpleVariableFactory();
}
