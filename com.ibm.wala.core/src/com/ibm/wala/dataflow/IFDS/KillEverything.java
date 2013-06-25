/*******************************************************************************
 * Licensed Materials - Property of IBM
 * 
 * "Restricted Materials of IBM"
 *
 * Copyright (c) 2007 IBM Corporation.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.intset.SparseIntSet;

/**
 * A flow function that kills everything (even 0)
 * 
 * @author sjfink
 */
public class KillEverything implements IUnaryFlowFunction {
  
  private final static KillEverything INSTANCE = new KillEverything();
  
  public static KillEverything singleton() { 
    return INSTANCE;
  }
  
  
  private KillEverything() {
  }

  @Override
  public SparseIntSet getTargets(int d1) {
    return null;
  }

}
