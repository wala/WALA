/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT;

/**
 * This class represents instanceof instructions.
 */
public final class InstanceofInstruction extends Instruction implements IInstanceofInstruction {
  final private String type;

  protected InstanceofInstruction(String type) {
    super(OP_instanceof);
    this.type = type;
    if (type == null) {
      throw new IllegalArgumentException("null type");
    }
  }

  public static InstanceofInstruction make(String type) {
    return new InstanceofInstruction(type);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof InstanceofInstruction) {
      InstanceofInstruction i = (InstanceofInstruction) o;
      return i.type.equals(type);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 31980190 + type.hashCode();
  }

  @Override
  public int getPoppedCount() {
    return 1;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getPushedType(String[] types) {
    return TYPE_boolean;
  }

  @Override
  public byte getPushedWordSize() {
    return 1;
  }

  @Override
  public void visit(IInstruction.Visitor v) throws IllegalArgumentException {
    if (v == null) {
      throw new IllegalArgumentException();
    }
    v.visitInstanceof(this);
  }

  @Override
  public String toString() {
    return "Instanceof(" + type + ")";
  }

  @Override
  public boolean isPEI() {
    return false;
  }

  @Override
  public boolean firstClassType() {
    return false;
  }
}
