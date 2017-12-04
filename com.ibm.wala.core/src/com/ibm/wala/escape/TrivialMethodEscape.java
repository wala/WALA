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

import java.util.Collections;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractLocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * Trivial method-level escape analysis.
 * 
 * An instance does not escape from method m if the following hold:
 * <ol>
 * <li>the instance is only ever pointed to by locals (it is never stored in the heap)
 * <li>the method m does NOT return (either normally or exceptionally) a pointer to the instance
 * </ol>
 */
public class TrivialMethodEscape implements IMethodEscapeAnalysis, INodeEscapeAnalysis {

  /**
   * Heap graph representation of pointer analysis
   */
  private final HeapGraph<InstanceKey> hg;

  /**
   * Governing call graph
   */
  private final CallGraph cg;

  /**
   * @param hg Heap graph representation of pointer analysis
   * @param cg governing call graph
   */
  public TrivialMethodEscape(CallGraph cg, HeapGraph<InstanceKey> hg) {
    this.hg = hg;
    this.cg = cg;
  }

  @Override
  public boolean mayEscape(MethodReference allocMethod, int allocPC, MethodReference m) throws WalaException {

    if (allocMethod == null) {
      throw new IllegalArgumentException("null allocMethod");
    }
    // nodes:= set of call graph nodes representing method m
    Set nodes = cg.getNodes(m);
    if (nodes.size() == 0) {
      throw new WalaException("could not find call graph node for method " + m);
    }

    // allocN := set of call graph nodes representing method allocMethod
    Set<CGNode> allocN = cg.getNodes(allocMethod);
    if (allocN.size() == 0) {
      throw new WalaException("could not find call graph node for allocation method " + allocMethod);
    }
    return mayEscape(allocN, allocPC, nodes);
  }

  @Override
  public boolean mayEscape(CGNode allocNode, int allocPC, CGNode node) throws WalaException {
    return mayEscape(Collections.singleton(allocNode), allocPC, Collections.singleton(node));
  }

  /**
   * @param allocN Set<CGNode> representing the allocation site.
   * @param allocPC
   * @param nodes Set<CGNode>, the nodes of interest
   * @return true iff some instance allocated at a site N \in &lt;allocN, allocPC> might escape from some activation of a node m \in
   *         { nodes }
   * @throws WalaException
   */
  private boolean mayEscape(Set<CGNode> allocN, int allocPC, Set nodes) throws WalaException {
    Set<InstanceKey> instances = HashSetFactory.make();
    // instances := set of instance key allocated at &lt;allocMethod, allocPC>
    for (CGNode n : allocN) {
      NewSiteReference site = findAlloc(n, allocPC);
      InstanceKey ik = hg.getHeapModel().getInstanceKeyForAllocation(n, site);
      if (ik == null) {
        throw new WalaException("could not get instance key at site " + site + " in " + n);
      }
      instances.add(ik);
    }

    for (InstanceKey ik : instances) {
      for (Object o : Iterator2Iterable.make(hg.getPredNodes(ik))) {
        PointerKey p = (PointerKey) o;
        if (!(p instanceof AbstractLocalPointerKey)) {
          // a pointer from the heap. give up.
          return true;
        } else {
          if (p instanceof ReturnValueKey) {
            ReturnValueKey rk = (ReturnValueKey) p;
            if (nodes.contains(rk.getNode())) {
              // some node representing method m returns the instance to its
              // caller
              return true;
            }
          }
        }
      }
    }

    // if we get here, it may not escape
    return false;
  }

  /**
   * @param n a call graph node
   * @param allocPC a bytecode index corresponding to an allocation
   * @return the NewSiteReference for the allocation
   * @throws WalaException
   */
  static NewSiteReference findAlloc(CGNode n, int allocPC) throws WalaException {
    if (n == null) {
      throw new IllegalArgumentException("null n");
    }
    for (NewSiteReference site : Iterator2Iterable.make(n.iterateNewSites())) {
      if (site.getProgramCounter() == allocPC) {
        return site;
      }
    }
    throw new WalaException("Failed to find an allocation at pc " + allocPC + " in node " + n);
  }

}
