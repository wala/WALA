/*
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.shrike.shrikeBT;

/** This class represents get and getstatic instructions. */
public class GetInstruction extends Instruction implements IGetInstruction {
  protected String type;

  protected String classType;

  protected String fieldName;

  GetInstruction(short opcode, String type, String classType, String fieldName) {
    super(opcode);
    this.type = type;
    this.classType = classType;
    this.fieldName = fieldName;
  }

  ConstantPoolReader getLazyConstantPool() {
    return null;
  }

  static final class Lazy extends GetInstruction {
    private final ConstantPoolReader cp;

    private final int index;

    Lazy(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, null, null, null);
      this.index = index;
      this.cp = cp;
    }

    @Override
    ConstantPoolReader getLazyConstantPool() {
      return cp;
    }

    int getCPIndex() {
      return index;
    }

    @Override
    public String getClassType() {
      if (classType == null) {
        classType = cp.getConstantPoolMemberClassType(index);
      }
      return classType;
    }

    @Override
    public String getFieldName() {
      if (fieldName == null) {
        fieldName = cp.getConstantPoolMemberName(index);
      }
      return fieldName;
    }

    @Override
    public String getFieldType() {
      if (type == null) {
        type = cp.getConstantPoolMemberType(index);
      }
      return type;
    }
  }

  static GetInstruction make(ConstantPoolReader cp, int index, boolean isStatic) {
    return new Lazy(isStatic ? OP_getstatic : OP_getfield, cp, index);
  }

  public static GetInstruction make(
      String type, String className, String fieldName, boolean isStatic) {
    if (type == null) {
      throw new IllegalArgumentException("type must not be null");
    }
    if (className == null) {
      throw new IllegalArgumentException("className must not be null");
    }
    if (fieldName == null) {
      throw new IllegalArgumentException("fieldName must not be null");
    }
    return new GetInstruction(isStatic ? OP_getstatic : OP_getfield, type, className, fieldName);
  }

  @Override
  public final boolean equals(Object o) {
    if (o instanceof GetInstruction) {
      GetInstruction i = (GetInstruction) o;
      return i.getFieldType().equals(getFieldType())
          && i.getClassType().equals(getClassType())
          && i.getFieldName().equals(getFieldName())
          && i.opcode == opcode;
    } else {
      return false;
    }
  }

  @Override
  public String getClassType() {
    return classType;
  }

  @Override
  public String getFieldName() {
    return fieldName;
  }

  @Override
  public String getFieldType() {
    return type;
  }

  @Override
  public final boolean isStatic() {
    return opcode == OP_getstatic;
  }

  @Override
  public final int hashCode() {
    return getClassType().hashCode()
        + 11113 * getFieldType().hashCode()
        + 398011 * getFieldName().hashCode()
        + opcode;
  }

  @Override
  public final int getPoppedCount() {
    return isStatic() ? 0 : 1;
  }

  @Override
  public final String getPushedType(String[] types) {
    return getFieldType();
  }

  @Override
  public final byte getPushedWordSize() {
    return Util.getWordSize(getFieldType());
  }

  @Override
  public String toString() {
    return "Get("
        + getFieldType()
        + ','
        + (isStatic() ? "STATIC" : "NONSTATIC")
        + ','
        + getClassType()
        + ','
        + getFieldName()
        + ')';
  }

  @Override
  public void visit(IInstruction.Visitor v) throws IllegalArgumentException {
    if (v == null) {
      throw new IllegalArgumentException();
    }
    v.visitGet(this);
  }

  @Override
  public boolean isPEI() {
    return !isStatic();
  }

  /** Java does not permit this */
  @Override
  public boolean isAddressOf() {
    return false;
  }
}
