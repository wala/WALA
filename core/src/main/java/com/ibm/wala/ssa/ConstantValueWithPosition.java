/**
 * ***************************************************************************** Copyright (c) 2007
 * IBM Corporation. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: IBM Corporation - initial API and implementation
 * *****************************************************************************
 */
package com.ibm.wala.ssa;

import java.util.Objects;

public class ConstantValueWithPosition extends ConstantValue {
  private final Object p;

  public ConstantValueWithPosition(Object constant, Object p) {
    super(constant);
    assert p != null;
    this.p = p;
  }

  public ConstantValueWithPosition(int constant, Object p) {
    super(constant);
    assert p != null;
    this.p = p;
  }

  public ConstantValueWithPosition(double constant, Object p) {
    super(constant);
    this.p = p;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(p);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    ConstantValueWithPosition other = (ConstantValueWithPosition) obj;
    return Objects.equals(p, other.p);
  }

  @Override
  public Object getPosition() {
    return p;
  }
}
