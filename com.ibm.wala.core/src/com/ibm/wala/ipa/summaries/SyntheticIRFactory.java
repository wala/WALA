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
package com.ibm.wala.ipa.summaries;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WarningSet;

public class SyntheticIRFactory implements IRFactory {

  public ControlFlowGraph makeCFG(IMethod method, Context C, ClassHierarchy cha, WarningSet warnings) {
    Assertions._assert(method.isSynthetic());
    SyntheticMethod sm = (SyntheticMethod) method;
    return sm.makeControlFlowGraph();
  }

  public IR makeIR(IMethod method, Context C, ClassHierarchy cha, SSAOptions options, WarningSet warnings) {
    if (method == null) {
      throw new IllegalArgumentException("method is null");
    }
    Assertions._assert(method.isSynthetic());
    SyntheticMethod sm = (SyntheticMethod) method;
    return sm.makeIR(options, warnings);
  }
}
