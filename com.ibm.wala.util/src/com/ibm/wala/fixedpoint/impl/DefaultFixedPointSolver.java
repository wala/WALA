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

import com.ibm.wala.fixpoint.IFixedPointSystem;
import com.ibm.wala.fixpoint.IVariable;


/**
 * Default implementation of a fixed point solver.
 */
public abstract class DefaultFixedPointSolver<T extends IVariable<?>> extends AbstractFixedPointSolver<T> {

  private final DefaultFixedPointSystem<T> graph;
  
  /**
   * @param expectedOut number of expected out edges in the "usual" case
   * for constraints .. used to tune graph representation
   */
  public DefaultFixedPointSolver(int expectedOut) {
    super();
    graph = new DefaultFixedPointSystem<>(expectedOut);
  }
  
  public DefaultFixedPointSolver() {
    super();
    graph = new DefaultFixedPointSystem<>();
  }
  
  @Override
  public IFixedPointSystem<T> getFixedPointSystem() {
    return graph;
  }
}
