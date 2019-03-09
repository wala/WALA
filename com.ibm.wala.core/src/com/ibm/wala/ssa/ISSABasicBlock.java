/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ssa;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.types.TypeReference;
import java.util.Iterator;

/** Common interface to all SSA BasicBlocks */
public interface ISSABasicBlock extends IBasicBlock<SSAInstruction> {

  /** Is this block a catch block */
  @Override
  public boolean isCatchBlock();

  /** Does this block represent the unique exit from a {@link ControlFlowGraph}? */
  @Override
  public boolean isExitBlock();

  /** Does this block represent the unique entry to a {@link ControlFlowGraph} */
  @Override
  public boolean isEntryBlock();

  /** @return the phi instructions incoming to this block */
  public Iterator<SSAPhiInstruction> iteratePhis();

  /** @return the pi instructions incoming to this block */
  public Iterator<SSAPiInstruction> iteratePis();

  /** @return the last instruction in this block. */
  public SSAInstruction getLastInstruction();

  /** @return the set of exception types this block may catch. */
  public Iterator<TypeReference> getCaughtExceptionTypes();
}
