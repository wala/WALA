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

import java.util.HashSet;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Heap;
import com.ibm.wala.util.debug.Assertions;

/**
 * Worklist for fixed-point solver implementation
 */
public class Worklist extends Heap {

  private final HashSet<AbstractStatement> contents = HashSetFactory.make();

  public Worklist() {
    super(100);
  }

  protected final boolean compareElements(Object o1, Object o2) {
    AbstractStatement eq1 = (AbstractStatement) o1;
    AbstractStatement eq2 = (AbstractStatement) o2;
    return (eq1.getOrderNumber() < eq2.getOrderNumber());
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.util.collections.Heap#take()
   */
  public Object take() {
    Assertions.UNREACHABLE();
    return null;
  }
  

  public AbstractStatement takeStatement() {
    AbstractStatement result = (AbstractStatement)super.take();
    contents.remove(result);
    return result;
  }
  
  public void insertStatement(AbstractStatement eq) {
    if (!contents.contains(eq)) {
      contents.add(eq);
      super.insert(eq);
    }
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.util.collections.Heap#insert(java.lang.Object)
   */
  public void insert(Object elt) {
    Assertions.UNREACHABLE();
  }
}