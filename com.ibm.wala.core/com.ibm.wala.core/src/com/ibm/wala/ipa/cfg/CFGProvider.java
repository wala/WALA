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
package com.ibm.wala.ipa.cfg;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * A trivial field-based pointer analysis solution, which only uses the
 * information of which types (classes) are live.
 * 
 * @author sfink
 */
public interface CFGProvider {

  /**
   * @param N a call graph node
   * @return a CFG that represents the node, or null if it's an unmodelled native method
   */
  public ControlFlowGraph getCFG(CGNode N, WarningSet warnings);

}
