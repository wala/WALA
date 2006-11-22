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
public final class InstanceofInstruction extends Instruction {
  private String type;

  protected InstanceofInstruction(String type) {
    this.type = type;
    this.opcode = OP_instanceof;
  }

  public static InstanceofInstruction make(String type) {
    return new InstanceofInstruction(type);
  }

  public boolean equals(Object o) {
    if (o instanceof InstanceofInstruction) {
      InstanceofInstruction i = (InstanceofInstruction) o;
      return i.type.equals(type);
    } else {
      return false;
    }
  }

  public int hashCode() {
    return 31980190 + type.hashCode();
  }

  public int getPoppedCount() {
    return 1;
  }

  public String getType() {
    return type;
  }

  public String getPushedType(String[] types) {
    return TYPE_boolean;
  }

  public byte getPushedWordSize() {
    return 1;
  }

  public void visit(Visitor v) {
    v.visitInstanceof(this);
  }

  public String toString() {
    return "Instanceof(" + type + ")";
  }
    /* (non-Javadoc)
   * @see com.ibm.domo.cfg.IInstruction#isPEI()
   */
  public boolean isPEI() {
    return false;
  }
}