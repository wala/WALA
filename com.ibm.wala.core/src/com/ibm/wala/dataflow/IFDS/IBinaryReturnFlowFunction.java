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
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.intset.SparseIntSet;

/**
 * A binary flow function corresponding to a return statements combining information from the call site and the exit site.
 * 
 * This function should be pairwise distributive for use with the Tabulation algorithm.
 * 
 * SJF: I have made this extend IFlowFunction to minimize damage to the extant class hierarchy. But calling super.getTargets() will
 * be a problem, so be very careful in how you implement and use this. The Tabulation solver will do the right thing.
 */
public interface IBinaryReturnFlowFunction extends IFlowFunction {

  /**
   * @param call_d factoid of the caller at the call site
   * @param exit_d factoid of the callee at the exit site
   * @return set of ret_d such that ({@literal <call_d, exit_d>}, ret_d) is an edge in this distributive function's graph representation, or
   *         null if there are none
   */
  public SparseIntSet getTargets(int call_d, int exit_d);
}
