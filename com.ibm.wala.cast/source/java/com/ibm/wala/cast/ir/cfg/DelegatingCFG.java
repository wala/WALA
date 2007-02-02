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

import com.ibm.wala.cfg.*;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.util.graph.*;
import com.ibm.wala.util.intset.BitVector;

import java.util.*;

public class DelegatingCFG extends AbstractNumberedGraph<IBasicBlock>
    implements ControlFlowGraph 
{

  protected final ControlFlowGraph parent;

  public DelegatingCFG(ControlFlowGraph parent) {
    this.parent = parent;
  }

  protected NodeManager getNodeManager() {
    return parent;
  }

  protected EdgeManager getEdgeManager() {
    return parent;
  }

  public IBasicBlock entry() { return parent.entry(); }

  public IBasicBlock exit() { return parent.exit(); }

  public BitVector getCatchBlocks() { return parent.getCatchBlocks(); }

  public IBasicBlock getBlockForInstruction(int index) {
    return parent.getBlockForInstruction( index );
  }

  public IInstruction[] getInstructions() { return parent.getInstructions(); }

  public int getProgramCounter(int index) {
    return parent.getProgramCounter( index );
  }

  public IMethod getMethod() { return parent.getMethod(); }

  public Collection getExceptionalSuccessors(IBasicBlock b) {
    return parent.getExceptionalSuccessors( b ); 
  }

  public Collection getNormalSuccessors(IBasicBlock b) {
    return parent.getNormalSuccessors( b ); 
  }
     
  public Collection getExceptionalPredecessors(IBasicBlock b) {
    return parent.getExceptionalPredecessors( b ); 
  }

  public Collection getNormalPredecessors(IBasicBlock b) {
    return parent.getNormalPredecessors( b ); 
  }

}
