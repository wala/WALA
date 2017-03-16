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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.rta.ContextInsensitiveRTAInterpreter;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;

/**
 * Default implementation of SSAContextInterpreter for context-insensitive analysis.
 */
public class ContextInsensitiveSSAInterpreter extends ContextInsensitiveRTAInterpreter implements SSAContextInterpreter {

  protected final AnalysisOptions options;

  public ContextInsensitiveSSAInterpreter(AnalysisOptions options, IAnalysisCacheView cache) {
    super(cache);
    this.options = options;
  }

  @Override
  public IR getIR(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    // Note: since this is context-insensitive, we cache an IR based on the
    // EVERYWHERE context
    return getAnalysisCache().getIR(node.getMethod(), Everywhere.EVERYWHERE);
  }
  
  @Override
  public IRView getIRView(CGNode node) {
    return getIR(node);
  }


  @Override
  public int getNumberOfStatements(CGNode node) {
    IR ir = getIR(node);
    return (ir == null) ? -1 : ir.getInstructions().length;
  }

  @Override
  public boolean recordFactoryType(CGNode node, IClass klass) {
    return false;
  }

  @Override
  public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode N) {
    IR ir = getIR(N);
    if (ir == null) {
      return null;
    } else {
      return ir.getControlFlowGraph();
    }
  }

  @Override
  public DefUse getDU(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    // Note: since this is context-insensitive, we cache an IR based on the
    // EVERYWHERE context
    return getAnalysisCache().getDefUse(getAnalysisCache().getIR(node.getMethod(), Everywhere.EVERYWHERE));
  }
}
