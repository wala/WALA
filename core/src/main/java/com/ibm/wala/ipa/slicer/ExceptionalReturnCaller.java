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
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

/**
 * A {@link Statement} representing the exceptional return value in a caller, immediately after
 * returning to the caller.
 */
public class ExceptionalReturnCaller extends StatementWithInstructionIndex
    implements ValueNumberCarrier {

  public ExceptionalReturnCaller(CGNode node, int callIndex) {
    super(node, callIndex);
  }

  @Override
  public int getValueNumber() {
    return getInstruction().getException();
  }

  @Override
  public SSAAbstractInvokeInstruction getInstruction() {
    return (SSAAbstractInvokeInstruction) super.getInstruction();
  }

  @Override
  public Kind getKind() {
    return Kind.EXC_RET_CALLER;
  }
}
