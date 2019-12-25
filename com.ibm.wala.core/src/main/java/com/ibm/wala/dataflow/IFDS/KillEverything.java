/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

/*
 * Licensed Materials - Property of IBM
 *
 * "Restricted Materials of IBM"
 *
 * Copyright (c) 2007 IBM Corporation.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.intset.SparseIntSet;

/**
 * A flow function that kills everything (even 0)
 *
 * @author sjfink
 */
public class KillEverything implements IUnaryFlowFunction {

  private static final KillEverything INSTANCE = new KillEverything();

  public static KillEverything singleton() {
    return INSTANCE;
  }

  private KillEverything() {}

  @Override
  public SparseIntSet getTargets(int d1) {
    return null;
  }
}
