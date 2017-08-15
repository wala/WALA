/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.fixpoint;

import com.ibm.wala.util.graph.impl.NodeWithNumber;

/**
 * Represents a single step in an iterative solver
 */
public abstract class AbstractStatement<T extends IVariable<T>, O extends AbstractOperator<T>> extends NodeWithNumber implements IFixedPointStatement<T>{

  public abstract O getOperator();

  /**
   * Subclasses must implement this, to prevent non-determinism.
   */
  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object o);

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("");
    if (getLHS() == null) {
      result.append("null ");
    } else {
      result.append(getLHS().toString());
      result.append(" ");
    }
    result.append(getOperator().toString());
    result.append(" ");
    for (int i = 0; i < getRHS().length; i++) {
      if (getRHS()[i] == null) {
        result.append("null");
      } else {
        result.append(getRHS()[i].toString());
      }
      result.append(" ");
    }
    return result.toString();
  }
  
  public final int getOrderNumber() {
    T lhs = getLHS();
    return (lhs == null) ? 0 : lhs.getOrderNumber();
  }

}
