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
package com.ibm.wala.fixedpoint.impl;

import com.ibm.wala.fixpoint.IFixedPointStatement;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.util.graph.impl.NodeWithNumber;

/**
 * Represents a single step in an iterative solver
 * 
 * @author Stephen Fink
 * @author Julian Dolby
 */
public abstract class AbstractStatement extends NodeWithNumber implements IFixedPointStatement{


  /**
   * Return the operator for this equation
   * 
   * @return the operator for this equation
   */
  public abstract AbstractOperator getOperator();

  /**
   * Subclasses must implement this, to prevent non-determinism.
   */
  public abstract int hashCode();

  public abstract boolean equals(Object o);

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
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
  
  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.fixpoint.AbstractEquation#getOrderNumber()
   */
  public final int getOrderNumber() {
    IVariable lhs = getLHS();
    return (lhs == null) ? 0 : lhs.getOrderNumber();
  }

}