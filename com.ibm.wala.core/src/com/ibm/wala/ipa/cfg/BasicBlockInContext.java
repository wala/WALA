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

import java.util.Iterator;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.graph.impl.NodeWithNumber;

/**
 * A helper class to make the ipcfg work correctly with context-sensitive call
 * graphs.
 * 
 * @author sfink
 */
public final class BasicBlockInContext extends NodeWithNumber implements IBasicBlock {
  private final ISSABasicBlock delegate;

  private final CGNode node;

  public BasicBlockInContext(CGNode node, ISSABasicBlock bb) {
    this.delegate = bb;
    this.node = node;
  }

  /*
   * @see com.ibm.wala.cfg.IBasicBlock#getFirstInstructionIndex()
   */
  public int getFirstInstructionIndex() {
    return delegate.getFirstInstructionIndex();
  }

  /*
   * @see com.ibm.wala.cfg.IBasicBlock#getLastInstructionIndex()
   */
  public int getLastInstructionIndex() {
    return delegate.getLastInstructionIndex();
  }
  
  /* 
   * @see com.ibm.wala.cfg.IBasicBlock#iterateAllInstructions()
   */
  public Iterator<IInstruction> iterator() {
    return delegate.iterator();
  }

  /*
   * @see com.ibm.wala.cfg.IBasicBlock#getMethod()
   */
  public IMethod getMethod() {
    return delegate.getMethod();
  }

  /*
   * @see com.ibm.wala.cfg.IBasicBlock#getNumber()
   */
  public int getNumber() {
    return delegate.getNumber();
  }

  /*
   * @see com.ibm.wala.cfg.IBasicBlock#isCatchBlock()
   */
  public boolean isCatchBlock() {
    return delegate.isCatchBlock();
  }

  /*
   * @see com.ibm.wala.cfg.IBasicBlock#isEntryBlock()
   */
  public boolean isEntryBlock() {
    return delegate.isEntryBlock();
  }

  /*
   * @see com.ibm.wala.cfg.IBasicBlock#isExitBlock()
   */
  public boolean isExitBlock() {
    return delegate.isExitBlock();
  }

  @Override
  public boolean equals(Object arg0) {
    if (arg0 instanceof BasicBlockInContext) {
      BasicBlockInContext other = (BasicBlockInContext) arg0;
      return delegate.equals(other.delegate) && node.equals(other.node);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return delegate.hashCode() + 229 * node.hashCode();
  }

  public ISSABasicBlock getDelegate() {
    return delegate;
  }

  public CGNode getNode() {
    return node;
  }

  @Override
  public String toString() {
    return delegate.toString() + "(node:" + node.getGraphNodeId() + ")";
  }

}
