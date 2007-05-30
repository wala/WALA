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

  public IR getIR(CGNode node, WarningSet warnings) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    // Note: since this is context-insensitive, we cache an IR based on the
    // EVERYWHERE context
    return options.getSSACache().findOrCreateIR(node.getMethod(), Everywhere.EVERYWHERE, cha, options.getSSAOptions(), warnings);
  }

  public int getNumberOfStatements(CGNode node, WarningSet warnings) {
    IR ir = getIR(node, warnings);
    return (ir == null) ? -1 : ir.getInstructions().length;
  }

  @Override
  public boolean recordFactoryType(CGNode node, IClass klass) {
    return false;
  }

  public ControlFlowGraph getCFG(CGNode N, WarningSet warnings) {
    IR ir = getIR(N, warnings);
    if (ir == null) {
      return null;
    } else {
      return ir.getControlFlowGraph();
    }
  }

  public DefUse getDU(CGNode node, WarningSet warnings) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    // Note: since this is context-insensitive, we cache an IR based on the
    // EVERYWHERE context
    return options.getSSACache().findOrCreateDU(node.getMethod(), Everywhere.EVERYWHERE, cha, options.getSSAOptions(), warnings);
  }
}
