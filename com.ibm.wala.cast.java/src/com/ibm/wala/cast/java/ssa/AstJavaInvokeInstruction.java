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

import com.ibm.wala.cast.ir.ssa.FixedParametersInvokeInstruction;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;

public class AstJavaInvokeInstruction extends FixedParametersInvokeInstruction {

  protected AstJavaInvokeInstruction(int results[], int[] params, int exception, CallSiteReference site) {
    super(results, params, exception, site);
  }

  public AstJavaInvokeInstruction(int result, int[] params, int exception, CallSiteReference site) {
    this(new int[] { result }, params, exception, site);
    SSAInvokeInstruction.assertParamsKosher(result, params, site);
  }

  /**
   * Constructor InvokeInstruction. This case for void return values
   */
  public AstJavaInvokeInstruction(int[] params, int exception, CallSiteReference site) {
    this(null, params, exception, site);
  }

  @Override
  protected SSAInstruction copyInstruction(SSAInstructionFactory insts, int results[], int[] params, int exception) {
    return ((AstJavaInstructionFactory) insts).JavaInvokeInstruction(results, params, exception, getCallSite());
  }

  @Override
  public void visit(IVisitor v) {
    ((AstJavaInstructionVisitor) v).visitJavaInvoke(this);
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return JavaLanguage.getNullPointerException();
  }

  @Override
  public int hashCode() {
    return (site.hashCode() * 7529) + (exception * 9823);
  }

}
