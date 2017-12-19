/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.cfg.exc;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * Interface to retrieve the result of the interprocedural analysis.
 * 
 * @author Juergen Graf &lt;graf@kit.edu&gt;
 */
public interface InterprocAnalysisResult<I, T extends IBasicBlock<I>> {

  /**
   * Returns the result of the interprocedural analysis for the given call graph node.
   */
  ExceptionPruningAnalysis<I, T> getResult(CGNode n);
  
  /**
   * Returns true iff an analysis result exists for the given call graph node.
   */
  boolean containsResult(CGNode n);
  
}
