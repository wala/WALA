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
 * This class represents put and putstatic instructions.
 */
public class PutInstruction extends Instruction {
  protected String type;
  protected String classType;
  protected String fieldName;

  PutInstruction(short opcode, String type, String classType, String fieldName) {
    this.type = type;
    this.classType = classType;
    this.fieldName = fieldName;
    this.opcode = opcode;
  }

  ConstantPoolReader getLazyConstantPool() {
    return null;
  }

  final static class Lazy extends PutInstruction {
    private ConstantPoolReader cp;
    private int index;

    Lazy(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, null, null, null);
      this.index = index;
      this.cp = cp;
    }

    ConstantPoolReader getLazyConstantPool() {
      return cp;
    }

    int getCPIndex() {
      return index;
    }

    public String getClassType() {
      if (classType == null) {
        classType = cp.getConstantPoolMemberClassType(index);
      }
      return classType;
    }

    public String getFieldName() {
      if (fieldName == null) {
        fieldName = cp.getConstantPoolMemberName(index);
      }
      return fieldName;
    }

    public String getFieldType() {
      if (type == null) {
        type = cp.getConstantPoolMemberType(index);
      }
      return type;
    }
  }

  static PutInstruction make(ConstantPoolReader cp, int index, boolean isStatic) {
    return new Lazy(isStatic ? OP_putstatic : OP_putfield, cp, index);
  }

  public static PutInstruction make(String type, String className, String fieldName, boolean isStatic) {
    if (type == null) {
      throw new IllegalArgumentException("type must not be null");
    }
    if (className == null) {
      throw new IllegalArgumentException("className must not be null");
    }
    if (fieldName == null) {
      throw new IllegalArgumentException("fieldName must not be null");
    }
    return new PutInstruction(isStatic ? OP_putstatic : OP_putfield, type, className, fieldName);
  }

  final public boolean equals(Object o) {
    if (o instanceof PutInstruction) {
      PutInstruction i = (PutInstruction) o;
      return i.getFieldType().equals(getFieldType()) && i.getClassType().equals(getClassType())
          && i.getFieldName().equals(getFieldName()) && i.opcode == opcode;
    } else {
      return false;
    }
  }

  public String getClassType() {
    return classType;
  }

  public String getFieldType() {
    return type;
  }

  public String getFieldName() {
    return fieldName;
  }

  final public boolean isStatic() {
    return opcode == OP_putstatic;
  }

  final public int hashCode() {
    return getClassType().hashCode() + 9011 * getClassType().hashCode() + 317 * getFieldName().hashCode() + opcode;
  }

  final public int getPoppedCount() {
    return isStatic() ? 1 : 2;
  }

  final public String toString() {
    return "Put(" + getFieldType() + "," + (isStatic() ? "STATIC" : "NONSTATIC") + "," + getClassType() + "," + getFieldName()
        + ")";
  }

  final public void visit(Visitor v) throws NullPointerException {
    v.visitPut(this);
  }
    /* (non-Javadoc)
   * @see com.ibm.domo.cfg.IInstruction#isPEI()
   */
  public boolean isPEI() {
    return true;
  }
}