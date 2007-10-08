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

/**
 * A view of a CFG that ignores exceptional edges
 */
public class ExceptionPrunedCFG {

  private static class ExceptionEdgePruner<T extends IBasicBlock> implements EdgeFilter<T>{
    private final ControlFlowGraph<T> cfg;

    ExceptionEdgePruner(ControlFlowGraph<T> cfg) {
      this.cfg = cfg;
    }

    public boolean hasNormalEdge(T src, T dst) {
      return cfg.getNormalSuccessors(src).contains(dst);
    }

    public boolean hasExceptionalEdge(T src, T dst) {
      return false;
    }
  };

  public static <T extends IBasicBlock> PrunedCFG<T> make(ControlFlowGraph<T> cfg) {
    return PrunedCFG.make(cfg, new ExceptionEdgePruner<T>(cfg));
  }
}

