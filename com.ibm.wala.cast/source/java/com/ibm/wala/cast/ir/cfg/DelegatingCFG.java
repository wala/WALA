/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ir.cfg;

import java.util.Collection;
import java.util.List;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.intset.BitVector;

public class DelegatingCFG<I, T extends IBasicBlock<I>> extends AbstractNumberedGraph<T> implements ControlFlowGraph<I, T> {

  protected final ControlFlowGraph<I, T> parent;

  public DelegatingCFG(ControlFlowGraph<I, T> parent) {
    this.parent = parent;
  }

  protected NodeManager<T> getNodeManager() {
    return parent;
  }

  protected EdgeManager<T> getEdgeManager() {
    return parent;
  }

  public T entry() {
    return parent.entry();
  }

  public T exit() {
    return parent.exit();
  }

  public BitVector getCatchBlocks() {
    return parent.getCatchBlocks();
  }

  public T getBlockForInstruction(int index) {
    return parent.getBlockForInstruction(index);
  }

  public I[] getInstructions() {
    return parent.getInstructions();
  }

  public int getProgramCounter(int index) {
    return parent.getProgramCounter(index);
  }

  public IMethod getMethod() {
    return parent.getMethod();
  }

  public List<T> getExceptionalSuccessors(T b) {
    return parent.getExceptionalSuccessors(b);
  }

  public Collection<T> getNormalSuccessors(T b) {
    return parent.getNormalSuccessors(b);
  }

  public Collection<T> getExceptionalPredecessors(T b) {
    return parent.getExceptionalPredecessors(b);
  }

  public Collection<T> getNormalPredecessors(T b) {
    return parent.getNormalPredecessors(b);
  }

}
