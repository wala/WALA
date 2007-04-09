/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.logic;

import java.util.Collection;
import java.util.Collections;


public class IntConstant implements IConstant {
  private final int val;
  
  private IntConstant(int val) {
    this.val = val;
  }
  
  public static IntConstant make(int val) {
    return new IntConstant(val);
  }
  
  public Kind getKind() {
    return Kind.CONSTANT;
   }

  public int getVal() {
    return val;
  }

  @Override
  public String toString() {
    return String.valueOf(val);
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + val;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final IntConstant other = (IntConstant) obj;
    if (val != other.val)
      return false;
    return true;
  }

  public Collection<Variable> getFreeVariables() {
    return Collections.emptySet();
  }
  
}
