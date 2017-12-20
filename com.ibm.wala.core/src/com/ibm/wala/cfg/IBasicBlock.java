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
import com.ibm.wala.util.graph.INodeWithNumber;

/**
 * An interface for a basic block in a control flow graph.
 */
public interface IBasicBlock<InstType> extends INodeWithNumber, Iterable<InstType> {

  /**
   * Get the index of the first instruction in the basic block.  The value
   * is an index into the instruction array that contains all the instructions
   * for the method.
   * 
   * If the result is &lt; 0, the block has no instructions
   * 
   * @return the instruction index for the first instruction in the basic block.
   */
  public int getFirstInstructionIndex();

  /**
   * Get the index of the last instruction in the basic block.  The value
   * is an index into the instruction array that contains all the instructions
   * for the method.
   * 
   * If the result is &lt; 0, the block has no instructions
   * 
   * @return the instruction index for the last instruction in the basic block
   */
  public int getLastInstructionIndex();
  

  /**
   * Return true if the basic block represents a catch block.
   * @return true if the basic block represents a catch block.
   */
  public boolean isCatchBlock();
  
  /**
   * Return true if the basic block represents the unique exit block.
   * @return true if the basic block represents the unique exit block.
   */
  public boolean isExitBlock();
  
  /**
   * Return true if the basic block represents the unique entry block.
   * @return true if the basic block represents the unique entry block.
   */
  public boolean isEntryBlock();
  
  /**
   * @return governing method for this block
   */
  public IMethod getMethod();

  /**
   * Each basic block should have a unique number in its cfg
   * @return the basic block's number
   */
  public int getNumber();

}
