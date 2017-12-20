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

package com.ibm.wala.cfg.exc.intra;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;

/**
 * A modifiable control flow graph.
 * 
 * @author Juergen Graf &lt;graf@kit.edu&gt;
 *
 */
public class MutableCFG<X, T extends IBasicBlock<X>> extends SparseNumberedGraph<T> implements ControlFlowGraph<X, T> {

  private final ControlFlowGraph<X, T> orig;
  
  private MutableCFG(final ControlFlowGraph<X, T> orig) {
    this.orig = orig;
  }
  
  public static <I, T extends IBasicBlock<I>> MutableCFG<I, T> copyFrom(ControlFlowGraph<I, T> cfg) {
    MutableCFG<I, T> mutable = new MutableCFG<>(cfg);
    
    for (T node : cfg) {
      mutable.addNode(node);
    }
    
    for (T node : cfg) {
      for (T succ : cfg.getNormalSuccessors(node)) {
        mutable.addEdge(node, succ);
      }

      for (T succ : cfg.getExceptionalSuccessors(node)) {
        mutable.addEdge(node, succ);
      }
    }
    
    return mutable;
  }

  @Override
  public T entry() {
    return orig.entry();
  }

  @Override
  public T exit() {
    return orig.exit();
  }

  // slow
  @Override
  public BitVector getCatchBlocks() {
    final BitVector bvOrig = orig.getCatchBlocks();
    final BitVector bvThis = new BitVector();
    
    for (final T block : this) {
      bvThis.set(block.getNumber());
    }
    
    bvThis.and(bvOrig);
    
    return bvThis;
  }

  @Override
  public T getBlockForInstruction(int index) {
    final T block =  orig.getBlockForInstruction(index);
    
    return (containsNode(block) ? block : null);
  }

  @Override
  public X[] getInstructions() {
    return orig.getInstructions();
  }

  @Override
  public int getProgramCounter(int index) {
    return orig.getProgramCounter(index);
  }

  @Override
  public IMethod getMethod() {
    return orig.getMethod();
  }

  @Override
  public List<T> getExceptionalSuccessors(T b) {
    final List<T> origSucc = orig.getExceptionalSuccessors(b);
    final IntSet allSuccs = this.getSuccNodeNumbers(b);
    final List<T> thisSuccs = new LinkedList<>();
    
    for (final T block : origSucc) {
      if (allSuccs.contains(block.getNumber())) {
        thisSuccs.add(block);
      }
    }
    
    return thisSuccs;
  }

  @Override
  public Collection<T> getNormalSuccessors(T b) {
    final List<T> excSuccs = getExceptionalSuccessors(b);
    final List<T> thisSuccs = new LinkedList<>();
    
    final Iterator<T> succs = getSuccNodes(b);
    while (succs.hasNext()) {
      final T succ = succs.next();
      if (!excSuccs.contains(succ)) {
        thisSuccs.add(succ);
      }
    }
    
    return thisSuccs;
  }

  @Override
  public Collection<T> getExceptionalPredecessors(T b) {
    final Collection<T> origPreds = orig.getExceptionalPredecessors(b);
    final IntSet allPreds = this.getPredNodeNumbers(b);
    final List<T> thisPreds = new LinkedList<>();
    
    for (final T block : origPreds) {
      if (allPreds.contains(block.getNumber())) {
        thisPreds.add(block);
      }
    }
    
    return thisPreds;
  }

  @Override
  public Collection<T> getNormalPredecessors(T b) {
    final Collection<T> excPreds = getExceptionalPredecessors(b);
    final List<T> thisPreds = new LinkedList<>();
    
    final Iterator<T> preds = getPredNodes(b);
    while (preds.hasNext()) {
      final T pred = preds.next();
      if (!excPreds.contains(pred)) {
        thisPreds.add(pred);
      }
    }
    
    return thisPreds;
  }
  
}
