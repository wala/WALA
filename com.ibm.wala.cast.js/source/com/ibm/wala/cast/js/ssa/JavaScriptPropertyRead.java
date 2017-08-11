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
package com.ibm.wala.cast.js.ssa;

import java.util.Collection;

import com.ibm.wala.cast.ir.ssa.AbstractReflectiveGet;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.TypeReference;

public class JavaScriptPropertyRead extends AbstractReflectiveGet {
  public JavaScriptPropertyRead(int iindex, int result, int objectRef, int memberRef) {
    super(iindex, result, objectRef, memberRef);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return
      ((JSInstructionFactory)insts).PropertyRead(iindex,
        defs==null? getDef(): defs[0],
	uses==null? getObjectRef(): uses[0],
	uses==null? getMemberRef(): uses[1]);
  }

  /* (non-Javadoc)
   * @see com.ibm.domo.ssa.Instruction#isPEI()
   */
  @Override
  public boolean isPEI() {
    return true;
  }

  /* (non-Javadoc)
   * @see com.ibm.domo.ssa.Instruction#getExceptionTypes()
   */
  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Util.typeErrorExceptions();
  }

  /**
  /* (non-Javadoc)
   * @see com.ibm.wala.ssa.SSAInstruction#visit(com.ibm.wala.ssa.SSAInstruction.IVisitor)
   */
  @Override
  public void visit(IVisitor v) {
    assert v instanceof JSInstructionVisitor;
    ((JSInstructionVisitor)v).visitJavaScriptPropertyRead(this);
  }
}
