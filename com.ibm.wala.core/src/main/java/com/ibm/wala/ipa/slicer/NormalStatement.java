/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.ipa.callgraph.CGNode;

/** A statement that has a corresponding index in the SSA IR */
public class NormalStatement extends StatementWithInstructionIndex {

  public NormalStatement(CGNode node, int instructionIndex) {
    super(node, instructionIndex);
  }

  @Override
  public Kind getKind() {
    return Kind.NORMAL;
  }

  @Override
  public String toString() {
    StringBuilder name = new StringBuilder();
    if (getInstruction().hasDef()) {
      String[] names =
          getNode().getIR().getLocalNames(getInstructionIndex(), getInstruction().getDef());
      if (names != null && names.length > 0) {
        name = new StringBuilder("[").append(names[0]);
        for (int i = 1; i < names.length; i++) {
          name.append(", ").append(names[i]);
        }
        name.append("]: ");
      }
    }

    return "NORMAL "
        + getNode().getMethod().getName()
        + ':'
        + name
        + getInstruction().toString()
        + ' '
        + getNode();
  }
}
