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

import com.ibm.wala.analysis.arraybounds.ArrayOutOfBoundsAnalysis;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.filter.ArrayOutOfBoundFilter;
import com.ibm.wala.ssa.SSAInstruction;

public class ArrayOutOfBoundInterFilter extends StoringExceptionFilter<SSAInstruction>{

  @Override
  protected ExceptionFilter<SSAInstruction> computeFilter(CGNode node) {
    ArrayOutOfBoundsAnalysis analysis = new ArrayOutOfBoundsAnalysis(node.getIR());
    return new ArrayOutOfBoundFilter(analysis);
  }

}
