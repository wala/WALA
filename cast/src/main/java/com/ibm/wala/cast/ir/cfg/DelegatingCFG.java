/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.ir.cfg;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.BitVector;
import java.util.Collection;
import java.util.List;

public class DelegatingCFG<I, T extends IBasicBlock<I>> extends AbstractNumberedGraph<T>
    implements ControlFlowGraph<I, T> {

  protected final ControlFlowGraph<I, T> parent;

  public DelegatingCFG(ControlFlowGraph<I, T> parent) {
    this.parent = parent;
  }

  @Override
  protected NumberedNodeManager<T> getNodeManager() {
    return parent;
  }

  @Override
  protected NumberedEdgeManager<T> getEdgeManager() {
    return parent;
  }

  @Override
  public T entry() {
    return parent.entry();
  }

  @Override
  public T exit() {
    return parent.exit();
  }

  @Override
  public BitVector getCatchBlocks() {
    return parent.getCatchBlocks();
  }

  @Override
  public T getBlockForInstruction(int index) {
    return parent.getBlockForInstruction(index);
  }

  @Override
  public I[] getInstructions() {
    return parent.getInstructions();
  }

  @Override
  public int getProgramCounter(int index) {
    return parent.getProgramCounter(index);
  }

  @Override
  public IMethod getMethod() {
    return parent.getMethod();
  }

  @Override
  public List<T> getExceptionalSuccessors(T b) {
    return parent.getExceptionalSuccessors(b);
  }

  @Override
  public Collection<T> getNormalSuccessors(T b) {
    return parent.getNormalSuccessors(b);
  }

  @Override
  public Collection<T> getExceptionalPredecessors(T b) {
    return parent.getExceptionalPredecessors(b);
  }

  @Override
  public Collection<T> getNormalPredecessors(T b) {
    return parent.getNormalPredecessors(b);
  }
}
