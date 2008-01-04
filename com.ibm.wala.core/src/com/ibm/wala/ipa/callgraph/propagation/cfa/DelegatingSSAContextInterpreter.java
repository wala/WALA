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
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.debug.Assertions;

/**
 * An {@link SSAContextInterpreter} that first checks with A, then defaults to B.
 * 
 * @author sfink
 */
public class DelegatingSSAContextInterpreter extends DelegatingRTAContextInterpreter implements SSAContextInterpreter {

  private final SSAContextInterpreter A;

  private final SSAContextInterpreter B;

  /**
   * TODO: really shouldn't allow A to be null.
   */
  public DelegatingSSAContextInterpreter(SSAContextInterpreter A, SSAContextInterpreter B) {
    super(A, B);
    this.A = A;
    this.B = B;
    if (Assertions.verifyAssertions) {
      Assertions._assert(B != null, "B is null");
    }
  }

  public IR getIR(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.getIR(node);
      }
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(B.understands(node));
    }
    return B.getIR(node);
  }

  public int getNumberOfStatements(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.getNumberOfStatements(node);
      }
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(B.understands(node));
    }
    return B.getNumberOfStatements(node);
  }

  @Override
  public boolean understands(CGNode node) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(B.understands(node));
    }
    return true;
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
  
  
  public ControlFlowGraph<ISSABasicBlock> getCFG(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.getCFG(node);
      }
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(B.understands(node));
    }
    return B.getCFG(node);
  }

  public DefUse getDU(CGNode node) {
    if (A != null) {
      if (A.understands(node)) {
        return A.getDU(node);
      }
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(B.understands(node));
    }
    return B.getDU(node);
  }
}
