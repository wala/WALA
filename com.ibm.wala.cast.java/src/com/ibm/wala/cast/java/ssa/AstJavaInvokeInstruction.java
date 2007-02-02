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
package com.ibm.wala.cast.java.ssa;

import java.util.Collection;

import com.ibm.wala.util.debug.Assertions;

import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.Exceptions;

public class AstJavaInvokeInstruction extends FixedParametersLexicalInvokeInstruction {

  public AstJavaInvokeInstruction(int result, int[] params, int exception, CallSiteReference site) {
    super(result, params, exception, site);
    if (Assertions.verifyAssertions) {
      SSAInvokeInstruction.assertParamsKosher(result, params, site);
    }
  }

  /**
   * Constructor InvokeInstruction. This case for void return values
   * @param i
   * @param params
   */
  public AstJavaInvokeInstruction(int[] params, int exception, CallSiteReference site) {
    this(-1, params, exception, site);
  }

  private AstJavaInvokeInstruction(int result, int[] params, int exception, CallSiteReference site, Access[] lexicalReads, Access[] lexicalWrites) {
    super(result, params, exception, site, lexicalReads, lexicalWrites);
    if (Assertions.verifyAssertions) {
      SSAInvokeInstruction.assertParamsKosher(result, params, site);
    }
  }
    
  protected SSAInstruction copyInstruction(int result, int[] params, int exception, Access[] lexicalReads, Access[] lexicalWrites) {
    return new AstJavaInvokeInstruction(result, params, exception, getCallSite(), lexicalReads, lexicalWrites);
  }

  /**
   * @see com.ibm.domo.ssa.SSAInstruction#visit(IVisitor)
   */
  public void visit(IVisitor v) {
    ((AstJavaInstructionVisitor)v).visitJavaInvoke(this);
  }

  /* (non-Javadoc)
   * @see com.ibm.domo.ssa.Instruction#getExceptionTypes()
   */
  public Collection getExceptionTypes() {
    return Exceptions.getNullPointerException();
  }

}
