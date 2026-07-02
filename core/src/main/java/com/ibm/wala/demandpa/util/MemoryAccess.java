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
 * @param instructionIndex index of the field access instruction in a shrikeBt or SSA instruction
 *     array
 */
public record MemoryAccess(int instructionIndex, CGNode node) {

  /**
   * @deprecated Use {@link #instructionIndex()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public int getInstructionIndex() {
    return instructionIndex();
  }

  @Override
  public String toString() {
    return "MemAccess: " + node() + ':' + instructionIndex();
  }

  /**
   * @deprecated Use {@link #node()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public CGNode getNode() {
    return node();
  }
}
