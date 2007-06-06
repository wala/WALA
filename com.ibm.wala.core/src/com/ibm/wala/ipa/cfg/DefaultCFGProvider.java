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

import com.ibm.wala.cfg.CFGCache;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * A CFG Provider that serves the ShrikeBT CFG if available, else returns a CFG
 * based on a context-insensitive SSA IR.
 */
public class DefaultCFGProvider implements CFGProvider {
  private final CallGraph callGraph;

  private final CFGCache cfgCache;

  public ControlFlowGraph getCFG(CGNode n, WarningSet warnings) {

    RTAContextInterpreter interp = callGraph.getInterpreter(n);
    if (interp instanceof CFGProvider) {
      return ((CFGProvider) interp).getCFG(n, warnings);
    } else {
      return cfgCache.findOrCreate(n.getMethod(), n.getContext(), warnings);
    }
  }

  public DefaultCFGProvider(CallGraph CG, CFGCache cfgCache) {
    this.callGraph = CG;
    this.cfgCache = cfgCache;
  }
}
