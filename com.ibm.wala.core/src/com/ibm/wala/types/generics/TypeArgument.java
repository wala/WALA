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
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * UNDER CONSTRUCTION
 * 
 * @author sjfink
 *
 */
public class TypeArgument {
  
  private final static TypeArgument WILDCARD = new TypeArgument("*") {
    public boolean isWildcard() {
      return true;
    }
  };
  
  private final String s;
  
  TypeArgument(String s) {
    this.s = s;
  }
  
  public boolean isWildcard() {
    return false;
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
    final TypeArgument other = (TypeArgument) obj;
    if (s == null) {
      if (other.s != null)
        return false;
    } else if (!s.equals(other.s))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return s;
  }

  public static TypeArgument make(String string) throws UnimplementedError {
    if (string.equals("*")) {
      return WILDCARD;
    } else {
      Assertions.UNREACHABLE("implement me");
      return null;
    }
  }

}
