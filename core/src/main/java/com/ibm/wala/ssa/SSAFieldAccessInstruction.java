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

import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;

/** Abstract base class for {@link SSAGetInstruction} and {@link SSAPutInstruction}. */
public abstract class SSAFieldAccessInstruction extends SSAInstruction {

  private final FieldReference field;

  private final int ref;

  protected SSAFieldAccessInstruction(int iindex, FieldReference field, int ref)
      throws IllegalArgumentException {
    super(iindex);
    this.field = field;
    this.ref = ref;
    if (field == null) {
      throw new IllegalArgumentException("field cannot be null");
    }
  }

  public TypeReference getDeclaredFieldType() {
    return field.getFieldType();
  }

  public FieldReference getDeclaredField() {
    return field;
  }

  public int getRef() {
    return ref;
  }

  public boolean isStatic() {
    return ref == -1;
  }

  @Override
  public boolean isPEI() {
    return !isStatic();
  }
}
