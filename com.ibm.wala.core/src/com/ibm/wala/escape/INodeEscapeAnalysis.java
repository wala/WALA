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
package com.ibm.wala.escape;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.WalaException;

/**
 * Basic interface from which to execute and get the results of escape analysis.
 */
public interface INodeEscapeAnalysis extends IMethodEscapeAnalysis {

  /**
   * @param allocNode a CGNode which holds an allocation site
   * @param allocPC bytecode index of allocation site
   * @param node method in question
   * @throws WalaException
   * @return true if an object allocated at the allocation site &lt;allocMethod,allocPC&gt; may escape from an activation of node m,
   *          false otherwise
   */
  public boolean mayEscape(CGNode allocNode, int allocPC, CGNode node) throws WalaException;

}
