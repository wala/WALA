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
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.impl.NodeWithNumber;

/**
 * A helper class to make the ipcfg work correctly with context-sensitive call
 * graphs.
 * 
 * @author sfink
 */
public final class BasicBlockInContext extends NodeWithNumber implements IBasicBlock {
  private final IBasicBlock delegate;

  private final CGNode node;

  public BasicBlockInContext(CGNode node, IBasicBlock bb) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(!(bb instanceof BasicBlockInContext));
    }
    this.delegate = bb;
    this.node = node;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.IBasicBlock#getFirstInstructionIndex()
   */
  public int getFirstInstructionIndex() {
    return delegate.getFirstInstructionIndex();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.IBasicBlock#getLastInstructionIndex()
   */
  public int getLastInstructionIndex() {
    return delegate.getLastInstructionIndex();
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.cfg.IBasicBlock#iterateAllInstructions()
   */
  public Iterator<IInstruction> iterator() {
    return delegate.iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.IBasicBlock#getMethod()
   */
  public IMethod getMethod() {
    return delegate.getMethod();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.IBasicBlock#getNumber()
   */
  public int getNumber() {
    return delegate.getNumber();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.IBasicBlock#isCatchBlock()
   */
  public boolean isCatchBlock() {
    return delegate.isCatchBlock();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.IBasicBlock#isEntryBlock()
   */
  public boolean isEntryBlock() {
    return delegate.isEntryBlock();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.IBasicBlock#isExitBlock()
   */
  public boolean isExitBlock() {
    return delegate.isExitBlock();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object arg0) {
    if (arg0 instanceof BasicBlockInContext) {
      BasicBlockInContext other = (BasicBlockInContext) arg0;
      return delegate.equals(other.delegate) && node.equals(other.node);
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return delegate.hashCode() + 229 * node.hashCode();
  }

  /**
   * @return Returns the delegate.
   */
  public IBasicBlock getDelegate() {
    return delegate;
  }

  /**
   * @return Returns the node.
   */
  public CGNode getNode() {
    return node;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return delegate.toString() + "(node:" + node.getGraphNodeId() + ")";
  }

}
