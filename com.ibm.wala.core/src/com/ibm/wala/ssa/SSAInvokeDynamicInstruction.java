/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;

public class SSAInvokeDynamicInstruction extends SSAInvokeInstruction {
  private final BootstrapMethod bootstrap;
  
  public SSAInvokeDynamicInstruction(int result, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap) {
    super(result, params, exception, site);
    this.bootstrap = bootstrap;
  }

  public SSAInvokeDynamicInstruction(int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap) {
    super(params, exception, site);
    this.bootstrap = bootstrap;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return insts.InvokeInstruction(defs == null || result == -1 ? result : defs[0], uses == null ? params : uses,
        defs == null ? exception : defs[result == -1 ? 0 : 1], site, bootstrap);
  }

  
}
