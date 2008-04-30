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
 * 
 * A function 0 -> 0, which kills all other incoming dataflow facts
 * 
 * TODO: optimize by building this edge as implicit in every flow function.
 * 
 * This is deprecated ... it's confusing and non-general.
 * 
 * @author sfink
 */
@Deprecated
public class UniversalKillFlowFunction implements IReversibleFlowFunction {

  private final static UniversalKillFlowFunction singleton = new UniversalKillFlowFunction();

  public SparseIntSet getTargets(int i) {
    return (i == 0) ? SparseIntSet.singleton(0) : null;
  }

  public SparseIntSet getSources(int i) {
    return (i == 0) ? SparseIntSet.singleton(0) : null;
  }

  /**
   * @return the singleton instance of this flow function
   */
  public static UniversalKillFlowFunction kill() {
    return singleton;
  }

  @Override
  public String toString() {
    return "Universal Kill";
  }
}