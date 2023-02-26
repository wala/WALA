/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.demandpa.util;

import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * * represents a single static occurrence of a memory access (i.e., an access to a field or to the
 * contents of an array) in the code
 *
 * @author sfink
 */
public class MemoryAccess {

  private final CGNode node;

  /** index of the field access instruction in a shrikeBt or SSA instruction array */
  private final int instructionIndex;

  public MemoryAccess(int index, CGNode node) {
    super();
    instructionIndex = index;
    this.node = node;
  }

  /** @return Returns the instructionIndex. */
  public int getInstructionIndex() {
    return instructionIndex;
  }

  @Override
  public String toString() {
    return "MemAccess: " + getNode() + ':' + getInstructionIndex();
  }

  /** @return Returns the node. */
  public CGNode getNode() {
    return node;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + instructionIndex;
    result = prime * result + ((node == null) ? 0 : node.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    MemoryAccess other = (MemoryAccess) obj;
    if (instructionIndex != other.instructionIndex) return false;
    if (node == null) {
      if (other.node != null) return false;
    } else if (!node.equals(other.node)) return false;
    return true;
  }
}
