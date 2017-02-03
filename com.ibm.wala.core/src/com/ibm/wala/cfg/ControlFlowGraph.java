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
package com.ibm.wala.cfg;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.intset.BitVector;

/**
 * An interface that is common to the Shrike and SSA CFG implementations.
 */
public interface ControlFlowGraph<I, T extends IBasicBlock<I>> extends NumberedGraph<T>, MinimalCFG<T> {

  /**
   * @return the indices of the catch blocks, as a bit vector
   */
  public BitVector getCatchBlocks();

  /**
   * @param index an instruction index
   * @return the basic block which contains this instruction.
   */
  public T getBlockForInstruction(int index);

  /**
   * @return the instructions of this CFG, as an array.
   */
  I[] getInstructions();

  /**
   * TODO: move this into IR?
   * 
   * @param index an instruction index
   * @return the program counter (bytecode index) corresponding to that instruction
   */
  public int getProgramCounter(int index);

  /**
   * @return the Method this CFG represents
   */
  public IMethod getMethod();
}
