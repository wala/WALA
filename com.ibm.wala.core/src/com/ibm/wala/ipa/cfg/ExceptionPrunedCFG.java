/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.cfg;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;

public class ExceptionPrunedCFG {

  private static final EdgeFilter exceptionEdgePruner = new EdgeFilter() {
    public boolean hasNormalEdge(IBasicBlock src, IBasicBlock dst) {
      return true;
    }
    public boolean hasExceptionalEdge(IBasicBlock src, IBasicBlock dst) {
      return false;
    }
  };

  public static PrunedCFG make(ControlFlowGraph cfg) {
    return PrunedCFG.make(cfg, exceptionEdgePruner);
  }
}

