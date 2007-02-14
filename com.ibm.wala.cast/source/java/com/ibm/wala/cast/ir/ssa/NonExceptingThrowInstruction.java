/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ir.ssa;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;

/**
 *  In some languages, the throw itself cannot throw exceptions other
 * than the argument.  This instruction represents such throws.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class NonExceptingThrowInstruction extends SSAAbstractThrowInstruction {

  public NonExceptingThrowInstruction(int exception) {
    super( exception);
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return new NonExceptingThrowInstruction(uses==null? getException(): uses[0]);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(Visitor)
   */
  public void visit(IVisitor v) {
    ((AstInstructionVisitor)v).visitNonExceptingThrow(this);
  }

  public Collection<TypeReference> getExceptionTypes() {
    return Collections.emptySet();
  }

}
