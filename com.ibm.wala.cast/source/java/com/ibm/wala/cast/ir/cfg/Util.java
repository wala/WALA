/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ir.cfg;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;

public class Util {

  public static <I, T extends IBasicBlock<I>> int whichPred(ControlFlowGraph<I, T> CFG, T Y, T X) {
    int i = 0;
    for (Object N : Iterator2Iterable.make(CFG.getPredNodes(Y))) {
      if (N == X)
        return i;
      ++i;
    }

    Assertions.UNREACHABLE();
    return -1;
  }

}
