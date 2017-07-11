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

import com.ibm.wala.util.graph.INodeWithNumber;


/**
 * Represents a single variable in a fixed-point iterative system.
 */
public interface IVariable<T extends IVariable<T>> extends INodeWithNumber {
  
  /**
   * Variables must allow the solver implementation to get/set an order number,
   * which the solver uses to control evaluation order.
   * 
   * It might be cleaner to hold this on the side, but we cannot tolerate any
   * extra space.  TODO: consider moving this functionality to a subinterface?
   * 
   * @return a number used to order equation evaluation
   */
  int getOrderNumber();

  /**
   * Variables must allow the solver implementation to get/set an order number,
   * which the solver uses to control evaluation order.
   * 
   * It might be cleaner to hold this on the side, but we cannot tolerate any
   * extra space.  TODO: consider moving this functionality to a subinterface?
   */
  public abstract void setOrderNumber(int i);

  /**
    * Set this variable to have the same state as another one
    */
  public void copyState(T v);

}
