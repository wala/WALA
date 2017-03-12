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
 * A flow function where out == in
 */
public class IdentityFlowFunction implements IReversibleFlowFunction {

  private final static IdentityFlowFunction singleton = new IdentityFlowFunction();

  @Override
  public SparseIntSet getTargets(int i) {
    return SparseIntSet.singleton(i);
  }

  @Override
  public SparseIntSet getSources(int i) {
    return SparseIntSet.singleton(i);
  }

  public static IdentityFlowFunction identity() {
    return singleton;
  }

  @Override
  public String toString() {
    return "Identity Flow";
  }

}
