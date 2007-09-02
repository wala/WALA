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

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.intset.BitVector;

public class DelegatingCFG extends AbstractNumberedGraph<IBasicBlock> implements ControlFlowGraph<IBasicBlock> {

  protected final ControlFlowGraph parent;

  public DelegatingCFG(ControlFlowGraph parent) {
    this.parent = parent;
  }

  protected NodeManager<IBasicBlock> getNodeManager() {
    return parent;
  }

  protected EdgeManager<IBasicBlock> getEdgeManager() {
    return parent;
  }

  public IBasicBlock entry() {
    return parent.entry();
  }

  public IBasicBlock exit() {
    return parent.exit();
  }

  public BitVector getCatchBlocks() {
    return parent.getCatchBlocks();
  }

  public IBasicBlock getBlockForInstruction(int index) {
    return parent.getBlockForInstruction(index);
  }

  public IInstruction[] getInstructions() {
    return parent.getInstructions();
  }

  public int getProgramCounter(int index) {
    return parent.getProgramCounter(index);
  }

  public IMethod getMethod() {
    return parent.getMethod();
  }

  public Collection<IBasicBlock> getExceptionalSuccessors(IBasicBlock b) {
    return parent.getExceptionalSuccessors(b);
  }

  public Collection<IBasicBlock> getNormalSuccessors(IBasicBlock b) {
    return parent.getNormalSuccessors(b);
  }

  public Collection<IBasicBlock> getExceptionalPredecessors(IBasicBlock b) {
    return parent.getExceptionalPredecessors(b);
  }

  public Collection<IBasicBlock> getNormalPredecessors(IBasicBlock b) {
    return parent.getNormalPredecessors(b);
  }

}
