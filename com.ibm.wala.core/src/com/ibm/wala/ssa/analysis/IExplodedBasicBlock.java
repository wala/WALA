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
package com.ibm.wala.ssa.analysis;

import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;

/**
 * A basic block with exactly one normal instruction (which may be null), corresponding to a single instruction index in the SSA
 * instruction array.
 *
 * The block may also have phis.
 */
public interface IExplodedBasicBlock extends ISSABasicBlock {

  /**
   * get the instruction for this block, or null if the block has no instruction
   */
  public SSAInstruction getInstruction();

  /**
   * if this represents an exception handler block, return the corresponding {@link SSAGetCaughtExceptionInstruction}
   *
   * @throws IllegalArgumentException if this does not represent an exception handler block
   */
  public SSAGetCaughtExceptionInstruction getCatchInstruction();

  /**
   * get the number of the original basic block containing the instruction of
   * this exploded block
   */
  public int getOriginalNumber();

}
