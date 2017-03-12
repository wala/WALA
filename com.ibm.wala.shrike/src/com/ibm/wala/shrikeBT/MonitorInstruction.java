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
 * This class represents monitorenter and monitorexit instructions.
 */
public final class MonitorInstruction extends Instruction {
  protected MonitorInstruction(short opcode) {
    super(opcode);
  }

  private final static MonitorInstruction enter = new MonitorInstruction(OP_monitorenter);

  private final static MonitorInstruction exit = new MonitorInstruction(OP_monitorexit);

  public static MonitorInstruction make(boolean entering) {
    return entering ? enter : exit;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof MonitorInstruction) {
      MonitorInstruction i = (MonitorInstruction) o;
      return i.opcode == opcode;
    } else {
      return false;
    }
  }

  public boolean isEnter() {
    return opcode == OP_monitorenter;
  }

  @Override
  public int hashCode() {
    return opcode + 1911;
  }

  @Override
  public int getPoppedCount() {
    return 1;
  }

  @Override
  public void visit(IInstruction.Visitor v) throws IllegalArgumentException {
    if (v == null) {
      throw new IllegalArgumentException();
    }
    v.visitMonitor(this);
  }

  @Override
  public String toString() {
    return "Monitor(" + (isEnter() ? "ENTER" : "EXIT") + ")";
  }

  @Override
  public boolean isPEI() {
    return true;
  }
}
