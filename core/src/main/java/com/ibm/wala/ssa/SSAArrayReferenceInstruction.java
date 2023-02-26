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

import com.ibm.wala.types.TypeReference;

/** Abstract base class for instructions that load or store from array contents. */
public abstract class SSAArrayReferenceInstruction extends SSAInstruction {

  private final int arrayref;

  private final int index;

  private final TypeReference elementType;

  SSAArrayReferenceInstruction(int iindex, int arrayref, int index, TypeReference elementType) {
    super(iindex);
    this.arrayref = arrayref;
    this.index = index;
    this.elementType = elementType;
    if (elementType == null) {
      throw new IllegalArgumentException("null elementType");
    }
  }

  @Override
  public int getNumberOfUses() {
    return 2;
  }

  @Override
  public int getUse(int j) {
    assert j <= 1;
    return (j == 0) ? arrayref : index;
  }

  /** Return the value number of the array reference. */
  public int getArrayRef() {
    return arrayref;
  }

  /** Return the value number of the index of the array reference. */
  public int getIndex() {
    return index;
  }

  public TypeReference getElementType() {
    return elementType;
  }

  /** @return true iff this represents an array access of a primitive type element */
  public boolean typeIsPrimitive() {
    return elementType.isPrimitiveType();
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }
}
