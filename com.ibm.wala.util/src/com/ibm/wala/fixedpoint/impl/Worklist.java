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
import java.util.NoSuchElementException;

import com.ibm.wala.fixpoint.AbstractStatement;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Heap;

/**
 * Worklist for fixed-point solver implementation
 */
@SuppressWarnings("rawtypes")
public class Worklist extends Heap<AbstractStatement> {

  private final HashSet<AbstractStatement> contents = HashSetFactory.make();

  public Worklist() {
    super(100);
  }

  @Override
  protected final boolean compareElements(AbstractStatement eq1, AbstractStatement eq2) {
    return (eq1.getOrderNumber() < eq2.getOrderNumber());
  }

  public AbstractStatement takeStatement() throws NoSuchElementException {
    AbstractStatement result = super.take();
    contents.remove(result);
    return result;
  }
  
  public void insertStatement(AbstractStatement eq) {
    if (!contents.contains(eq)) {
      contents.add(eq);
      super.insert(eq);
    }
  }

}
