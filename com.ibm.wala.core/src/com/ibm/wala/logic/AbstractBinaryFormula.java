/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.logic;

import com.ibm.wala.logic.ILogicConstants.BinaryConnective;

public abstract class AbstractBinaryFormula implements IFormula {

  protected AbstractBinaryFormula() {
    super();
  }

  public final Kind getKind() {
    return Kind.BINARY;
  }

  public abstract BinaryConnective getConnective();

  public abstract IFormula getF1();

  public abstract IFormula getF2();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();
  
  
}
