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
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.rta.ContextInsensitiveRTAInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * Default implementation of SSAContextInterpreter for context-insensitive
 * analysis
 * 
 * @author sfink
 */
public class ContextInsensitiveSSAInterpreter extends ContextInsensitiveRTAInterpreter implements SSAContextInterpreter {

  private final AnalysisOptions options;

  private final ClassHierarchy cha;

  public ContextInsensitiveSSAInterpreter(AnalysisOptions options, ClassHierarchy cha) {
    this.options = options;
    this.cha = cha;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.detox.ipa.util.CFAContextInterpreter#getIR()
   */
  public IR getIR(CGNode node, WarningSet warnings) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    // Note: since this is context-insensitive, we cache an IR based on the
    // EVERYWHERE context
    return options.getSSACache().findOrCreateIR(node.getMethod(), Everywhere.EVERYWHERE, cha, options.getSSAOptions(), warnings);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.detox.ipa.underConstruction.CFAContextInterpreter#getNumberOfStatements(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.detox.ipa.callgraph.Context)
   */
  public int getNumberOfStatements(CGNode node, WarningSet warnings) {
    IR ir = getIR(node, warnings);
    return (ir == null) ? -1 : ir.getInstructions().length;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.cfa.CFAContextInterpreter#recordFactoryType(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.IClass)
   */
  public boolean recordFactoryType(CGNode node, IClass klass) {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.cfg.CFGProvider#getCFG(com.ibm.wala.ipa.callgraph.CGNode)
   */
  public ControlFlowGraph getCFG(CGNode N, WarningSet warnings) {
    IR ir = getIR(N, warnings);
    if (ir == null) {
      return null;
    } else {
      return ir.getControlFlowGraph();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getDU(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public DefUse getDU(CGNode node, WarningSet warnings) {
    // Note: since this is context-insensitive, we cache an IR based on the
    // EVERYWHERE context
    return options.getSSACache().findOrCreateDU(node.getMethod(), Everywhere.EVERYWHERE, cha, options.getSSAOptions(), warnings);
  }
}
