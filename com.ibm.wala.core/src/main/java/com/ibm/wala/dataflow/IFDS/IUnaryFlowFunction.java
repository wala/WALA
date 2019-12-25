/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.intset.IntSet;

/**
 * A flow function corresponding to an edge in the supergraph.
 *
 * <p>This function should be distributive for use with the Tabulation algorithm.
 */
public interface IUnaryFlowFunction extends IFlowFunction {

  /**
   * @return set of d2 such that (d1,d2) is an edge in this distributive function's graph
   *     representation, or null if there are none
   */
  public IntSet getTargets(int d1);
}
