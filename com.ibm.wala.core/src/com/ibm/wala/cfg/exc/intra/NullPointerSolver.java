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
package com.ibm.wala.cfg.exc.intra;

import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;

/**
 * Intraprocedural dataflow analysis to detect impossible NullPointerExceptions.
 * 
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class NullPointerSolver<B extends ISSABasicBlock> extends DataflowSolver<B, NullPointerState> {

  private final int maxVarNum;
  private final ParameterState parameterState;
  private final IR ir;

  public NullPointerSolver(NullPointerFrameWork<B> problem, int maxVarNum, int[] paramVarNum, IR ir) {
    this(problem, maxVarNum, paramVarNum, ParameterState.createDefault(ir.getMethod()), ir);
  }
  
  public NullPointerSolver(NullPointerFrameWork<B> problem, int maxVarNum, int[] paramVarNum, ParameterState initialState, IR ir) {
    super(problem);
    this.maxVarNum = maxVarNum;
    this.parameterState = initialState;
    this.ir = ir;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.graph.DataflowSolver#makeEdgeVariable(java.lang.Object, java.lang.Object)
   */
  @Override
  protected NullPointerState makeEdgeVariable(B src, B dst) {
    return new NullPointerState(maxVarNum, ir.getSymbolTable(), parameterState);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.dataflow.graph.DataflowSolver#makeNodeVariable(java.lang.Object, boolean)
   */
  @Override
  protected NullPointerState makeNodeVariable(B n, boolean IN) {
    return new NullPointerState(maxVarNum, ir.getSymbolTable(), parameterState);
  }

  @Override
  protected NullPointerState[] makeStmtRHS(int size) {
    return new NullPointerState[size];
  }
  
}
