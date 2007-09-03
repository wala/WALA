/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/package com.ibm.wala.analysis.typeInference;

import com.ibm.wala.fixedpoint.impl.AbstractVariable;

/**
 * A type variable in the dataflow system.
 */
public class TypeVariable extends AbstractVariable {

  TypeAbstraction type;

  private final int hash;

  public TypeVariable(TypeAbstraction type, int hashCode) {
    this.type = type;
    this.hash = hashCode;
  }

  public void copyState(TypeVariable other) throws IllegalArgumentException {
    if (other == null) {
      throw new IllegalArgumentException("v == null");
    }
    this.type = other.type;
  }


  public TypeAbstraction getType() {
    return type;
  }

  public void setType(TypeAbstraction type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return type.toString();
  }

  @Override
  public int hashCode() {
    return hash;
  }

}