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
package com.ibm.wala.analysis.exceptionanalysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;

public class IntraproceduralResult {
  private Set<TypeReference> exceptions;
  private Map<CGNode, Set<TypeReference>> intraproceduralExceptions;
  private CallGraph callGraph;

  public IntraproceduralResult(CallGraph cg) {
    this.callGraph = cg;
    intraproceduralExceptions = new HashMap<CGNode, Set<TypeReference>>();
    exceptions = new HashSet<>();
    compute();
  }

  private void compute() {
    for (CGNode node : callGraph) {
      intraproceduralExceptions.put(node, new HashSet<TypeReference>());

      IR ir = node.getIR();
      if (ir != null) {
        for (ISSABasicBlock block : ir.getControlFlowGraph()) {
          if (block.getLastInstructionIndex() >= 0) {
            SSAInstruction lastInstruction = block.getLastInstruction();
            intraproceduralExceptions.get(node).addAll(lastInstruction.getExceptionTypes());
          }
          // TODO: Add Throw, Analyze catch
        }
      }
    }
    
    for (Set<TypeReference> exceptions :intraproceduralExceptions.values()) {
      this.exceptions.addAll(exceptions);
    }
  }

  public Set<TypeReference> getFilteredExceptions(CGNode node, CallSiteReference callsite) {
    assert (node.getIR().getInstructions()[callsite.getProgramCounter()] instanceof SSAInvokeInstruction);
    return Collections.emptySet();
  }

  public Set<TypeReference> getIntraproceduralExceptions(CGNode node) {
    if (!callGraph.containsNode(node)) {
      throw new IllegalArgumentException("The given CG node has to be part " + "of the call graph given during construction.");
    }

    Set<TypeReference> result = intraproceduralExceptions.get(node);
    if (result == null) {
      throw new RuntimeException("Internal Error: No result for the given node.");
    }
    return result;
  }

  public Set<TypeReference> getExceptions() {
    return exceptions;
  }
}
