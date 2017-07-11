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
 * Represents a single variable in a fixed-point system.
 */
public abstract class AbstractVariable<T extends AbstractVariable<T>> extends NodeWithNumber implements IVariable<T> {

  private static int nextHashCode = 0;
  
  private int orderNumber;
  
  private final int hashCode;
  
  protected AbstractVariable() {
    this.hashCode = nextHash();
  }

  @Override
  public boolean equals(Object obj) {
    // we assume the solver manages these canonically
    return this == obj;
  }
  
  /**
   * I know this is theoretically bad.   However,
   * <ul>
   * <li> we need this to be extremely fast .. it's in the inner loop of lots of stuff.
   * <li> these objects will probably only be hashed with each other {@link AbstractVariable}s, 
   * in which case incrementing hash codes is OK.
   * <li> we want determinism, so we don't want to rely on System.identityHashCode
   * </ul>
   */
  public static synchronized int nextHash() {
    return nextHashCode++;
  }

  @Override
  public final int hashCode() {
    return hashCode;
  }

  @Override
  public int getOrderNumber() {
    return orderNumber;
  }

  @Override
  public void setOrderNumber(int orderNumber) {
    this.orderNumber = orderNumber;
  }
}
