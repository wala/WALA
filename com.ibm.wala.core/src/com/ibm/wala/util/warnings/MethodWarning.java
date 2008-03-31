/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.warnings;

import com.ibm.wala.types.MemberReference;

/**
 *
 * A warning associated with a method
 * 
 * @author sfink
 */
public abstract class MethodWarning extends Warning {

  private final MemberReference method;

  public MethodWarning(byte level, MemberReference method) {
    super(level);
    this.method = method;
  }
  
  public MethodWarning(MemberReference method) {
    super();
    this.method = method;
  }

  public MemberReference getMethod() {
    return method;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((method == null) ? 0 : method.hashCode());
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
    final MethodWarning other = (MethodWarning) obj;
    if (method == null) {
      if (other.method != null)
        return false;
    } else if (!method.equals(other.method))
      return false;
    return true;
  }
  
}
