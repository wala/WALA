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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;

/**
 * An object that provides an interface to local method information needed for CFA.
 */
public interface SSAContextInterpreter extends RTAContextInterpreter {

  /**
   * @return the IR that models the method context, or null if it's an unmodelled native method
   */
  public IR getIR(CGNode node);

  public IRView getIRView(CGNode node);

  /**
   * @return DefUse for the IR that models the method context, or null if it's an unmodelled native method
   */
  public DefUse getDU(CGNode node);

  /**
   * @return the number of the statements in the IR, or -1 if it's an unmodelled native method.
   */
  public int getNumberOfStatements(CGNode node);

  public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode n);

}
