/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ssa;

/** The value of a constant which appears in an SSA IR. */
public class ConstantValue implements Value {
  private final Object constant;

  public ConstantValue(Object constant) {
    this.constant = constant;
  }

  public ConstantValue(int constant) {
    this(Integer.valueOf(constant));
  }

  public ConstantValue(double constant) {
    this(Double.valueOf(constant));
  }

  /** @return an object which represents the constant value */
  public Object getValue() {
    return constant;
  }

  @Override
  public String toString() {
    return "#" + constant;
  }

  @Override
  public boolean isStringConstant() {
    return constant instanceof String;
  }

  /** @return true iff this constant is "false" */
  public boolean isFalseConstant() {
    return (constant instanceof Boolean) && constant.equals(Boolean.FALSE);
  }

  /** @return true iff this constant is "true" */
  public boolean isTrueConstant() {
    return (constant instanceof Boolean) && constant.equals(Boolean.TRUE);
  }

  /** @return true iff this constant is "zero" */
  public boolean isZeroConstant() {
    return ((constant instanceof Number) && (((Number) constant).intValue() == 0));
  }

  /** @return true iff this constant is "null" */
  @Override
  public boolean isNullConstant() {
    return (constant == null);
  }

  /** @return true iff this constant is "one" */
  public boolean isOneConstant() {
    return ((constant instanceof Number) && (((Number) constant).intValue() == 1));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj.getClass().equals(getClass())) {
      ConstantValue other = (ConstantValue) obj;
      if (constant == null) {
        return other.constant == null;
      }
      return constant.equals(other.constant);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return constant == null ? 74 : 91 * constant.hashCode();
  }
}
