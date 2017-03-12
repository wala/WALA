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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.rta.DelegatingRTAContextInterpreter;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;

/**
 * An {@link SSAContextInterpreter} that first checks with A, then defaults to B.
 */
public class DelegatingSSAContextInterpreter extends DelegatingRTAContextInterpreter implements SSAContextInterpreter {

  private final SSAContextInterpreter A;

  private final SSAContextInterpreter B;

  /**
   * neither A nor B should be null.
   */
  public DelegatingSSAContextInterpreter(SSAContextInterpreter A, SSAContextInterpreter B) {
    super(A, B);
    this.A = A;
    this.B = B;
    if (A == null) {
      throw new IllegalArgumentException("A cannot be null");
    }
    if (B == null) {
      throw new IllegalArgumentException("B cannot be null");
    }
  }

  @Override
  public IR getIR(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.getIR(node);
      }
    }
    assert B.understands(node);
    return B.getIR(node);
  }

  @Override
  public IRView getIRView(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.getIRView(node);
      }
    }
    assert B.understands(node);
    return B.getIRView(node);
  }

  @Override
  public int getNumberOfStatements(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.getNumberOfStatements(node);
      }
    }
    assert B.understands(node);
    return B.getNumberOfStatements(node);
  }

  @Override
  public boolean understands(CGNode node) {
    return A.understands(node) || B.understands(node);
  }

  @Override
  public boolean recordFactoryType(CGNode node, IClass klass) {
    boolean result = false;
    if (A != null) {
      result |= A.recordFactoryType(node, klass);
    }
    result |= B.recordFactoryType(node, klass);
    return result;
  }

  @Override
  public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.getCFG(node);
      }
    }
    assert B.understands(node);
    return B.getCFG(node);
  }

  @Override
  public DefUse getDU(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.getDU(node);
      }
    }
    assert B.understands(node);
    return B.getDU(node);
  }
}
