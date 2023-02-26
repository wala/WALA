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
package com.ibm.wala.ipa.cfg;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.graph.impl.NodeWithNumber;
import java.util.Iterator;

/** A helper class to make the ipcfg work correctly with context-sensitive call graphs. */
public final class BasicBlockInContext<T extends ISSABasicBlock> extends NodeWithNumber
    implements ISSABasicBlock {
  private final T delegate;

  private final CGNode node;

  public BasicBlockInContext(CGNode node, T bb) {
    if (bb == null) {
      throw new IllegalArgumentException("null bb");
    }
    this.delegate = bb;
    this.node = node;
  }

  /** @see com.ibm.wala.cfg.IBasicBlock#getFirstInstructionIndex() */
  @Override
  public int getFirstInstructionIndex() {
    return delegate.getFirstInstructionIndex();
  }

  /** @see com.ibm.wala.cfg.IBasicBlock#getLastInstructionIndex() */
  @Override
  public int getLastInstructionIndex() {
    return delegate.getLastInstructionIndex();
  }

  /** @see com.ibm.wala.cfg.IBasicBlock#iterator() */
  @Override
  public Iterator<SSAInstruction> iterator() {
    return delegate.iterator();
  }

  /** @see com.ibm.wala.cfg.IBasicBlock#getMethod() */
  @Override
  public IMethod getMethod() {
    return delegate.getMethod();
  }

  /** @see com.ibm.wala.cfg.IBasicBlock#getNumber() */
  @Override
  public int getNumber() {
    return delegate.getNumber();
  }

  /** @see com.ibm.wala.cfg.IBasicBlock#isCatchBlock() */
  @Override
  public boolean isCatchBlock() {
    return delegate.isCatchBlock();
  }

  /** @see com.ibm.wala.cfg.IBasicBlock#isEntryBlock() */
  @Override
  public boolean isEntryBlock() {
    return delegate.isEntryBlock();
  }

  /** @see com.ibm.wala.cfg.IBasicBlock#isExitBlock() */
  @Override
  public boolean isExitBlock() {
    return delegate.isExitBlock();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
    result = prime * result + ((node == null) ? 0 : node.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final BasicBlockInContext<?> other = (BasicBlockInContext<?>) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    if (node == null) {
      if (other.node != null) return false;
    } else if (!node.equals(other.node)) return false;
    return true;
  }

  public T getDelegate() {
    return delegate;
  }

  public CGNode getNode() {
    return node;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public Iterator<TypeReference> getCaughtExceptionTypes() {
    return delegate.getCaughtExceptionTypes();
  }

  @Override
  public SSAInstruction getLastInstruction() {
    return delegate.getLastInstruction();
  }

  @Override
  public Iterator<SSAPhiInstruction> iteratePhis() {
    return delegate.iteratePhis();
  }

  @Override
  public Iterator<SSAPiInstruction> iteratePis() {
    return delegate.iteratePis();
  }
}
