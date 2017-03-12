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
 * This class represents checkcast instructions.
 */
final public class CheckCastInstruction extends Instruction implements ITypeTestInstruction {
  final private String type;

  protected CheckCastInstruction(String type) {
    super(OP_checkcast);
    this.type = type;
  }

  public static CheckCastInstruction make(String type) {
    if (type == null) {
      throw new IllegalArgumentException("type is null");
    }
    return new CheckCastInstruction(type.intern());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof CheckCastInstruction) {
      CheckCastInstruction i = (CheckCastInstruction) o;
      return i.type.equals(type);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 131111 + type.hashCode();
  }

  @Override
  public int getPoppedCount() {
    return 1;
  }

  /**
   * @return the type to which the operand is cast
   */
  @Override
  public String[] getTypes() {
    return new String[]{ type };
  }

  @Override
  public String getPushedType(String[] types) {
    return type;
  }

  @Override
  public byte getPushedWordSize() {
    return 1;
  }

  @Override
  public void visit(IInstruction.Visitor v) {
    if (v == null) {
      throw new IllegalArgumentException();
    }
    v.visitCheckCast(this);
  }

  @Override
  public String toString() {
    return "CheckCast(" + type + ")";
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  @Override
  public boolean firstClassTypes() {
    return false;
  }
}
