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
 * A flow function which has only the edge 0 -&gt; dest
 */
public class SingletonFlowFunction implements IReversibleFlowFunction {

	final private static SparseIntSet zeroSet = SparseIntSet.singleton(0);

	final int dest;
	
	private SingletonFlowFunction(int dest) {
		this.dest = dest;
	}

  @Override
  public SparseIntSet getTargets(int i) {
  	if (i == 0) {
      return SparseIntSet.add(zeroSet,dest);
  	} else {
  		return null;
  	}
  }
  
  @Override
  public SparseIntSet getSources(int i) {
   	if (i == dest || i == 0) { 
      return zeroSet;
   	} else {
   		return null;
   	}
  }

  public static SingletonFlowFunction create(int dest) {
    return new SingletonFlowFunction(dest);
  }

}
