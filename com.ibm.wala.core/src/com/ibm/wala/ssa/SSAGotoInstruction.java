/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import java.util.Collection;

/**
 * @author sfink
 *
 */
public class SSAGotoInstruction extends SSAInstruction {

  SSAGotoInstruction() {
    super();
  }
  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return
      new SSAGotoInstruction();
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    return "goto";
  }
  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitGoto(this);
  }

  public int hashCode() {
      return 1409; // XXX weak!
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  public boolean isFallThrough() {
    return false;
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  public Collection getExceptionTypes() {
    return null;
  }
}
