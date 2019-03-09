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
 * A flow function corresponding to an edge in the supergraph. A reversible flow-function supports a
 * getSources operation that allows computing backwards flow. At the very least, this is required in
 * IFDS by call functions for which sources need to be found to handle insertion of summary edges.
 */
public interface IReversibleFlowFunction extends IUnaryFlowFunction {

  /**
   * @return set of d1 such that (d1,d2) is an edge in this distributive function's graph
   *     representation, or null if there are none
   */
  public IntSet getSources(int d2);
}
