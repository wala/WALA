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

/**
 * A {@link Statement} representing a formal parameter
 */
public class ParamCallee extends Statement implements ValueNumberCarrier {
  /**
   * Value number of the parameter
   */
  protected final int valueNumber;

  public ParamCallee(CGNode node, int valueNumber) {
    super(node);
    this.valueNumber = valueNumber;
  }

  @Override
  public Kind getKind() {
    return Kind.PARAM_CALLEE;
  }

  @Override
  public int getValueNumber() {
    return valueNumber;
  }

  @Override
  public String toString() {
    return super.toString() + " v" + valueNumber;
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
    final ParamCallee other = (ParamCallee) obj;
    if (valueNumber != other.valueNumber)
      return false;
    return true;
  }
  
  
}
