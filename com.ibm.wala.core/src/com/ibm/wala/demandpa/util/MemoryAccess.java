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
package com.ibm.wala.demandpa.util;

import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * @author sfink
 * 
 * represents a single static occurence of a memory access (i.e., an access to a field 
 * or to the contents of an array) in the code
 */
public class MemoryAccess {

  final private CGNode node;

  /**
   * index of the field access instruction in a shrikeBt or SSA instruction
   * array
   */
  final int instructionIndex;

  public MemoryAccess(int index, CGNode node) {
    super();
    instructionIndex = index;
    this.node = node;
  }

  /**
   * @return Returns the instructionIndex.
   */
  public int getInstructionIndex() {
    return instructionIndex;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MemAccess: " + getNode() + ":" + getInstructionIndex();
  }

  /**
   * @return Returns the node.
   */
  public CGNode getNode() {
    return node;
  }

}
