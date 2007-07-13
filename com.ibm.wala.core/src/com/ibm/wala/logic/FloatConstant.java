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

public class FloatConstant extends AbstractConstant {
  private final float val;
  
  protected FloatConstant(float val) {
    this.val = val;
  }
  
  public static FloatConstant make(float val) {
    return new FloatConstant(val);
  }
  
  public float getVal() {
    return val;
  }

  @Override
  public String toString() {
    return String.valueOf(val);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Float.floatToIntBits(val);
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
    final FloatConstant other = (FloatConstant) obj;
    if (Float.floatToIntBits(val) != Float.floatToIntBits(other.val))
      return false;
    return true;
  }


}
