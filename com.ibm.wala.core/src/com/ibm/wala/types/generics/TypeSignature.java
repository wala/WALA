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
package com.ibm.wala.types.generics;

import com.ibm.wala.util.debug.Assertions;

/**
 * UNDER CONSTRUCTION
 * 
 * @author sjfink
 * 
 */
public abstract class TypeSignature {
  
  private final String s;
  
  TypeSignature(String s) {
    this.s = s;
  }
  
  protected String rawString() {
    return s;
  }

  public static TypeSignature make(String s) {
    if (s.charAt(0) == 'L') {
      return ClassTypeSignature.makeClassTypeSig(s);
    } else {
      Assertions.UNREACHABLE();
      return null;
    }
  }
  
  public abstract TypeArgument[] getTypeArguments();

  @Override
  public String toString() {
    return s;
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + ((s == null) ? 0 : s.hashCode());
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
    final TypeSignature other = (TypeSignature) obj;
    if (s == null) {
      if (other.s != null)
        return false;
    } else if (!s.equals(other.s))
      return false;
    return true;
  }

 
  public abstract boolean isTypeVariable();
}
