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
package com.ibm.wala.analysis.reflection;

import java.util.Map;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction.Visitor;
import com.ibm.wala.util.collections.HashMapFactory;

/**
 * A mapping from {@link CallSiteReference} to {@link SSAInvokeInstruction}
 * 
 * @deprecated since no one currently uses this.
 */
@Deprecated
public class CallSiteMap {

  /** 
   * f: CallSiteReference -> InvokeInstruction 
   */
  private final Map<CallSiteReference, SSAInvokeInstruction> map = HashMapFactory.make();

  /**
   * @throws IllegalArgumentException  if ir is null
   */
  public CallSiteMap(final IR ir) {
    if (ir == null) {
      throw new IllegalArgumentException("ir is null");
    }
    Visitor v = new Visitor() {
      @Override
      public void visitInvoke(SSAInvokeInstruction instruction) {
        CallSiteReference site = instruction.getCallSite();
        map.put(site, instruction);
      }
    };
    SSAInstruction[] instructions = ir.getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      if (instructions[i] != null) {
        instructions[i].visit(v);
      }
    }
  }

  /**
   * @return the InvokeInstruction corresponding to the call site.
   */
  public SSAInvokeInstruction getInstructionForSite(CallSiteReference site) {
    SSAInvokeInstruction result = map.get(site);
    return result;
  }
}
