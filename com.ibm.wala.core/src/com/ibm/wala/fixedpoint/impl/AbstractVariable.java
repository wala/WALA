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

import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.util.graph.impl.NodeWithNumber;

/**
 * Represents a single variable in a fixed-point system.
 * 
 * @author Stephen Fink
 */
public abstract class AbstractVariable extends NodeWithNumber implements IVariable {

  private int orderNumber;
 

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    // we assume the solver manages these canonically
    return this == obj;
  }
  

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public abstract int hashCode();
  
  
  /**
   * @return Returns the orderNumber.
   */
  public int getOrderNumber() {
    return orderNumber;
  }
  /**
   * @param orderNumber The orderNumber to set.
   */
  public void setOrderNumber(int orderNumber) {
    this.orderNumber = orderNumber;
  }
}