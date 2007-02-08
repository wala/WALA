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

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Exceptions;

/**
 * @author sfink
 *
 */
public class SSAThrowInstruction extends SSAAbstractThrowInstruction {

  public SSAThrowInstruction(int exception) {
    super(exception);
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) throws IllegalArgumentException {
    if (uses != null && uses.length != 1) {
      throw new IllegalArgumentException("if non-null, uses.length must be 1");
    }
    return new SSAThrowInstruction(uses==null? getException(): uses[0]);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    v.visitThrow(this);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  public Collection<TypeReference> getExceptionTypes() {
    return Exceptions.getNullPointerException();
  }
}
