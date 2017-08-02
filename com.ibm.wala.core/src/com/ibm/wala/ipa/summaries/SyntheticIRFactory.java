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
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;

public class SyntheticIRFactory implements IRFactory<SyntheticMethod> {

  public ControlFlowGraph makeCFG(SyntheticMethod method) {
    if (method == null) {
      throw new IllegalArgumentException("method is null");
    }
    return method.makeControlFlowGraph(method.getStatements());
  }

  @Override
  public IR makeIR(SyntheticMethod method, Context C, SSAOptions options) {
    if (method == null) {
      throw new IllegalArgumentException("method is null");
    }
    return method.makeIR(C, options);
  }

  @Override
  public boolean contextIsIrrelevant(SyntheticMethod method) {
    // conservatively return false .. the context might matter.
    return false;
  }
}
