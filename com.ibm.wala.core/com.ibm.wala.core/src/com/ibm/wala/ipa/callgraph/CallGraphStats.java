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
package com.ibm.wala.ipa.callgraph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethodWrapper;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * @author sfink
 * 
 */
public class CallGraphStats {

  /**
   * Constructor for CallGraphStats.
   */
  public static String getStats(CallGraph cg) {
    Set<CGNode> reachableNodes = DFS.getReachableNodes(cg, Collections.singleton(cg.getFakeRootNode()));
    int nNodes = 0;
    int nEdges = 0;
    for (Iterator<CGNode> it = reachableNodes.iterator(); it.hasNext();) {
      CGNode n = (CGNode) it.next();
      nNodes++;
      nEdges += cg.getSuccNodeCount(n);
    }
    StringBuffer result = new StringBuffer();
    result.append("Call graph stats:");
    result.append("\n");
    result.append("  Nodes: " + nNodes);
    result.append("\n");
    result.append("  Edges: " + nEdges);
    result.append("\n");
    result.append("  Methods: " + collectMethods(cg).size());
    result.append("\n");
    result.append("  Bytecode Bytes: " + countBytecodeBytes(cg));
    result.append("\n");
    return result.toString();
  }
  
  /**
   * @param cg
   * @return the number of bytecode bytes
   */
  public static int countBytecodeBytes(CallGraph cg) {
    int ret = 0;
    HashSet<IMethod> counted = HashSetFactory.make();
    for (Iterator<? extends CGNode> iter = cg.iterateNodes(); iter.hasNext();) {
      CGNode node = iter.next();
      IMethod method = node.getMethod();
      if (counted.add(method)) {
        if (method instanceof ShrikeCTMethodWrapper) {
          byte[] bytecodes = ((ShrikeCTMethodWrapper) method).getBytecodes();
          if (bytecodes != null) {
            ret += bytecodes.length;
          }
        }
      }
    }
    return ret;
  }

  /**
   * Walk the call graph and return the set of MethodReferences that appear in
   * the graph.
   * 
   * @param cg
   * @return a set of MethodReferences
   */
  public static Set<MethodReference> collectMethods(CallGraph cg) {
    HashSet<MethodReference> result = HashSetFactory.make();
    for (Iterator it = cg.iterateNodes(); it.hasNext();) {
      CGNode N = (CGNode) it.next();
      result.add(N.getMethod().getReference());
    }
    return result;
  }
}
