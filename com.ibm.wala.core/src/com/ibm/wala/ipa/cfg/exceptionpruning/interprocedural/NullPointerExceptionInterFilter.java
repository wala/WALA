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
package com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural;

import com.ibm.wala.analysis.nullpointer.IntraproceduralNullPointerAnalysis;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.filter.NullPointerExceptionFilter;
import com.ibm.wala.ssa.SSAInstruction;

public class NullPointerExceptionInterFilter extends StoringExceptionFilter<SSAInstruction>{

  @Override
  protected ExceptionFilter<SSAInstruction> computeFilter(CGNode node) {
    IntraproceduralNullPointerAnalysis analysis = new IntraproceduralNullPointerAnalysis(node.getIR());
    return new NullPointerExceptionFilter(analysis);
  }

}
