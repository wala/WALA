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
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * Collect basic call graph statistics
 */
public class CallGraphStats {

  public static class CGStats {

    private final int nNodes;

    private final int nEdges;

    private final int nMethods;

    private final int bytecodeBytes;

    private CGStats(int nodes, int edges, int methods, int bytecodeBytes) {
      super();
      nNodes = nodes;
      nEdges = edges;
      nMethods = methods;
      this.bytecodeBytes = bytecodeBytes;
    }

    public int getNNodes() {
      return nNodes;
    }

    public int getNEdges() {
      return nEdges;
    }

    public int getNMethods() {
      return nMethods;
    }

    public int getBytecodeBytes() {
      return bytecodeBytes;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + bytecodeBytes;
      result = prime * result + nEdges;
      result = prime * result + nMethods;
      result = prime * result + nNodes;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      CGStats other = (CGStats) obj;
      if (bytecodeBytes != other.bytecodeBytes)
        return false;
      if (nEdges != other.nEdges)
        return false;
      if (nMethods != other.nMethods)
        return false;
      if (nNodes != other.nNodes)
        return false;
      return true;
    }

    @Override
    public String toString() {
      StringBuffer result = new StringBuffer();
      result.append("Call graph stats:");
      result.append("\n");
      result.append("  Nodes: " + nNodes);
      result.append("\n");
      result.append("  Edges: " + nEdges);
      result.append("\n");
      result.append("  Methods: " + nMethods);
      result.append("\n");
      result.append("  Bytecode Bytes: " + bytecodeBytes);
      result.append("\n");
      return result.toString();

    }
  }

  public static CGStats getCGStats(CallGraph cg) {
    if (cg == null) {
      throw new IllegalArgumentException("cg is null");
    }
    Set<CGNode> reachableNodes = DFS.getReachableNodes(cg, Collections.singleton(cg.getFakeRootNode()));
    int nNodes = 0;
    int nEdges = 0;
    for (CGNode n : reachableNodes) {
      nNodes++;
      nEdges += cg.getSuccNodeCount(n);
    }
    return new CGStats(nNodes, nEdges, collectMethods(cg).size(), countBytecodeBytes(cg));
  }

  /**
   * @throws IllegalArgumentException if cg is null
   */
  public static String getStats(CallGraph cg) {
    return getCGStats(cg).toString();
  }

  /**
   * @param cg
   * @return the number of bytecode bytes
   * @throws IllegalArgumentException if cg is null
   */
  public static int countBytecodeBytes(CallGraph cg) {
    if (cg == null) {
      throw new IllegalArgumentException("cg is null");
    }
    int ret = 0;
    HashSet<IMethod> counted = HashSetFactory.make();
    for (CGNode node : cg) {
      IMethod method = node.getMethod();
      if (counted.add(method)) {
        if (method instanceof ShrikeCTMethod) {
          byte[] bytecodes = ((ShrikeCTMethod) method).getBytecodes();
          if (bytecodes != null) {
            ret += bytecodes.length;
          }
        }
      }
    }
    return ret;
  }

  /**
   * Walk the call graph and return the set of MethodReferences that appear in the graph.
   * 
   * @param cg
   * @return a set of MethodReferences
   * @throws IllegalArgumentException if cg is null
   */
  public static Set<MethodReference> collectMethods(CallGraph cg) {
    if (cg == null) {
      throw new IllegalArgumentException("cg is null");
    }
    HashSet<MethodReference> result = HashSetFactory.make();
    for (CGNode N : cg) {
      result.add(N.getMethod().getReference());
    }
    return result;
  }
}
