/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.ipa.summaries;

import com.ibm.wala.cast.js.cfg.JSInducedCFG;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;

public class JavaScriptSummarizedFunction extends SummarizedMethod {

  public JavaScriptSummarizedFunction(
      MethodReference ref, MethodSummary summary, IClass declaringClass) {
    super(ref, summary, declaringClass);
  }

  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  @Override
  public InducedCFG makeControlFlowGraph(SSAInstruction[] instructions) {
    return new JSInducedCFG(instructions, this, Everywhere.EVERYWHERE);
  }
}
