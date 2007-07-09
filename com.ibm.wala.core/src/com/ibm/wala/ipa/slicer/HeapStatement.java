/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

public abstract class HeapStatement extends Statement {

  private final PointerKey loc;

  public HeapStatement(CGNode node, PointerKey loc) {

    super(node);
    if (loc == null) {
      throw new IllegalArgumentException("loc is null");
    }
    this.loc = loc;
  }


  public final static class ParamCaller extends HeapStatement {
    // index into the IR instruction array of the call statements
    private final int callIndex;

    public ParamCaller(CGNode node,int callIndex, PointerKey loc) {
      super(node, loc);
      this.callIndex = callIndex;
    }

    @Override
    public Kind getKind() {
      return Kind.HEAP_PARAM_CALLER;
    }

    public int getCallIndex() {
      return callIndex;
    }
    
    public SSAAbstractInvokeInstruction getCall() {
      return (SSAAbstractInvokeInstruction) getNode().getIR().getInstructions()[callIndex];
    }
    
    @Override
    public String toString() {
      return getKind().toString() + ":" + getNode() + " " + getLocation() + " call:" + getCall();
    }

    @Override
    public int hashCode() {
      return getLocation().hashCode() + 4289 * getCall().hashCode() + 4133 * getNode().hashCode() + 8831;
    }

    @Override
    public boolean equals(Object obj) {
      // instanceof is OK because this class is final.  instanceof is more efficient than getClass
      if (obj instanceof ParamCaller) {
        ParamCaller other = (ParamCaller) obj;
        return getNode().equals(other.getNode()) && getLocation().equals(other.getLocation()) && callIndex == other.callIndex;
      } else {
        return false;
      }
    }
  }

  public final static class ParamCallee extends HeapStatement {
    public ParamCallee(CGNode node, PointerKey loc) {
      super(node, loc);
    }

    @Override
    public Kind getKind() {
      return Kind.HEAP_PARAM_CALLEE;
    }
    
    @Override
    public int hashCode() {
      return getLocation().hashCode() + 7727 * getNode().hashCode() + 7841;
    }

    @Override
    public boolean equals(Object obj) {
      // instanceof is ok because this class is final.  instanceof is more efficient than getClass
      if (obj instanceof ParamCallee) {
        ParamCallee other = (ParamCallee) obj;
        return getNode().equals(other.getNode()) && getLocation().equals(other.getLocation());
      } else {
        return false;
      }
    }
    
    @Override
    public String toString() {
      return getKind().toString() + ":" + getNode() + " " + getLocation();
    }
  }

  public final static class ReturnCaller extends HeapStatement {
    // index into the instruction array of the relevant call instruction
    private final int callIndex;
//    private final SSAAbstractInvokeInstruction call;

    public ReturnCaller(CGNode node, int callIndex, PointerKey loc) {
      super(node, loc);
      this.callIndex = callIndex;
    }

    @Override
    public Kind getKind() {
      return Kind.HEAP_RET_CALLER;
    }

    public int getCallIndex() {
      return callIndex;
    }
    
    public SSAAbstractInvokeInstruction getCall() {
      return (SSAAbstractInvokeInstruction) getNode().getIR().getInstructions()[callIndex];
    }

    @Override
    public String toString() {
      return getKind().toString() + ":" + getNode() + " " + getLocation() + " call:" + getCall();
    }

    @Override
    public int hashCode() {
      return getLocation().hashCode() + 8887 * getCall().hashCode() + 8731 * getNode().hashCode() + 7919;
    }

    @Override
    public boolean equals(Object obj) {    
      // instanceof is ok because this class is final.  instanceof is more efficient than getClass
      if (obj instanceof ReturnCaller) {
        ReturnCaller other = (ReturnCaller) obj;
        return getNode().equals(other.getNode()) && getLocation().equals(other.getLocation()) && callIndex == other.callIndex;
      } else {
        return false;
      }
    }
  }

  public final static class ReturnCallee extends HeapStatement {
    public ReturnCallee(CGNode node, PointerKey loc) {
      super(node, loc);
    }

    @Override
    public Kind getKind() {
      return Kind.HEAP_RET_CALLEE;
    }
    
    @Override
    public int hashCode() {
      return getLocation().hashCode() + 9533 * getNode().hashCode() + 9631;
    }

    @Override
    public boolean equals(Object obj) {
      // instanceof is ok because this class is final.  instanceof is more efficient than getClass
      if (obj instanceof ReturnCallee) {
        ReturnCallee other = (ReturnCallee) obj;
        return getNode().equals(other.getNode()) && getLocation().equals(other.getLocation());
      } else {
        return false;
      }
    }
    
    @Override
    public String toString() {
      return getKind().toString() + ":" + getNode() + " " + getLocation();
    }
  }

  public PointerKey getLocation() {
    return loc;
  }
}
