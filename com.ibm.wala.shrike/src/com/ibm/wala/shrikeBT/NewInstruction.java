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

public final class NewInstruction extends Instruction {
  final private String type;

  final private short arrayBoundsCount;

  protected NewInstruction(short opcode, String type, short arrayBoundsCount) {
    super(opcode);
    this.type = type;
    this.arrayBoundsCount = arrayBoundsCount;
  }

  /**
   * @param type the type of the object that will be returned (in JVM format, e.g., [Ljava/lang/String;)
   * @param arrayBoundsCount the number of array dimensions to preconstruct (equal to the number of integer parameters this
   *          instruction expects)
   * @throws IllegalArgumentException if type is null
   */
  public static NewInstruction make(String type, int arrayBoundsCount) throws IllegalArgumentException {
    if (type == null) {
      throw new IllegalArgumentException("type is null");
    }
    if (arrayBoundsCount < 0 || arrayBoundsCount > 255) {
      throw new IllegalArgumentException("Too many array bounds: " + arrayBoundsCount);
    } else {
      if (type.length() < arrayBoundsCount + 1) {
        throw new IllegalArgumentException("Not enough array nesting in " + type + " for bounds count " + arrayBoundsCount);
      }
      for (int i = 0; i < arrayBoundsCount; i++) {
        if (type.charAt(i) != '[') {
          throw new IllegalArgumentException("Not enough array nesting in " + type + " for bounds count " + arrayBoundsCount);
        }
      }

      short opcode;

      if (arrayBoundsCount == 0) {
        opcode = OP_new;
      } else if (arrayBoundsCount == 1) {
        char ch = type.charAt(1);
        if (ch != 'L' && ch != '[') {
          // array of primitive type
          opcode = OP_newarray;
        } else {
          opcode = OP_anewarray;
        }
      } else {
        opcode = OP_multianewarray;
      }
      return new NewInstruction(opcode, type, (short) arrayBoundsCount);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof NewInstruction) {
      NewInstruction i = (NewInstruction) o;
      return i.type.equals(type) && i.arrayBoundsCount == arrayBoundsCount;
    } else {
      return false;
    }
  }

  public int getArrayBoundsCount() {
    return arrayBoundsCount;
  }

  @Override
  public int hashCode() {
    return 13111143 * type.hashCode() + arrayBoundsCount;
  }

  @Override
  public int getPoppedCount() {
    return arrayBoundsCount;
  }

  @Override
  public String getPushedType(String[] types) {
    return type;
  }

  @Override
  public byte getPushedWordSize() {
    return 1;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return "New(" + type + "," + arrayBoundsCount + ")";
  }

  @Override
  public void visit(IInstruction.Visitor v) throws IllegalArgumentException {
    if (v == null) {
      throw new IllegalArgumentException();
    }
    v.visitNew(this);
  }

  @Override
  public boolean isPEI() {
    return true;
  }
}
