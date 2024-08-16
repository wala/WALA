/*
 * Copyright (c) 2024 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ssa;

/**
 * Unspecified Instructions are opaque values containing a payload of type T. Their CAst
 * representation is a primitive node with the payload as a constant value.
 *
 * @param <T> The type of the payload.
 */
public class SSAUnspecifiedInstruction<T> extends SSAInstruction {
  private final T payload;

  public SSAUnspecifiedInstruction(int iindex, T payload) {
    super(iindex);
    this.payload = payload;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return this;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return payload.toString();
  }

  @Override
  public void visit(IVisitor v) {
    v.visitUnspecified(this);
  }

  @Override
  public int hashCode() {
    return payload.hashCode();
  }

  public T getPayload() {
    return payload;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }
}
