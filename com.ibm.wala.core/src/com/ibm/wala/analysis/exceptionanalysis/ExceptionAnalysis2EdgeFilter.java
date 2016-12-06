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
package com.ibm.wala.analysis.exceptionanalysis;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cfg.EdgeFilter;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;

/**
 * Converter to use the results of the exception analysis with an edge filter.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class ExceptionAnalysis2EdgeFilter implements EdgeFilter<ISSABasicBlock> {
  private ExceptionAnalysis analysis;
  private CGNode node;

  public ExceptionAnalysis2EdgeFilter(ExceptionAnalysis analysis, CGNode node) {
    this.analysis = analysis;
    this.node = node;
  }

  @Override
  public boolean hasNormalEdge(ISSABasicBlock src, ISSABasicBlock dst) {
    boolean originalEdge = node.getIR().getControlFlowGraph().getNormalSuccessors(src).contains(dst);
    boolean result = originalEdge;
    SSAInstruction instruction = IntraproceduralExceptionAnalysis.getThrowingInstruction(src);
    if (instruction != null) {
      if (analysis.getFilter().getFilter(node).alwaysThrowsException(instruction)) {
        result = false;
      }
    }
    return result;
  }

  @Override
  public boolean hasExceptionalEdge(ISSABasicBlock src, ISSABasicBlock dst) {
    boolean originalEdge = node.getIR().getControlFlowGraph().getExceptionalSuccessors(src).contains(dst);
    boolean result = originalEdge;

    if (dst.isCatchBlock()) {
      if (!analysis.catchesException(node, src, dst)) {
        result = false;
      }
    } else {
      assert dst.isExitBlock();
      result = analysis.hasUncaughtExceptions(node, src);
    }
    return result;
  }

}
