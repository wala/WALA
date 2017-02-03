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
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

/**
 * A {@link Statement} representing an actual parameter
 */
public class ParamCaller extends StatementWithInstructionIndex implements ValueNumberCarrier {
  /**
   * Value number of the actual parameter
   */
  protected final int valueNumber;

  public ParamCaller(CGNode node, int callIndex, int valueNumber) {
    super(node, callIndex);
    this.valueNumber = valueNumber;
  }

  @Override
  public Kind getKind() {
    return Kind.PARAM_CALLER;
  }

  @Override
  public SSAAbstractInvokeInstruction getInstruction() {
    return (SSAAbstractInvokeInstruction)super.getInstruction();
  }

  @Override
  public String toString() {
    return super.toString() +  " v" + getValueNumber();
  }

  @Override
  public int getValueNumber() {
    return valueNumber;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + valueNumber;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    final ParamCaller other = (ParamCaller) obj;
    if (valueNumber != other.valueNumber)
      return false;
    return true;
  }


}
