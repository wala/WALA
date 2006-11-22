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
import com.ibm.wala.ssa.SSAInvokeInstruction;

/**
 * Represents parameter-passing statement in the SDG
 * 
 * @author sjfink
 *
 */
public abstract class ParamStatement extends Statement {

  public ParamStatement(CGNode node) {
    super(node);
  }

  public interface ValueNumberCarrier {
    public int getValueNumber();
  }
  
  public interface CallStatementCarrier {
    public SSAInvokeInstruction getCall();
  }

  public static class ParamCaller extends ParamStatement implements ValueNumberCarrier, CallStatementCarrier {
    private final SSAInvokeInstruction call;

    protected final int valueNumber;

    public ParamCaller(CGNode node, SSAInvokeInstruction call, int valueNumber) {
      super(node);
      this.call = call;
      this.valueNumber = valueNumber;
    }

    @Override
    public Kind getKind() {
      return Kind.PARAM_CALLER;
    }

    public SSAInvokeInstruction getCall() {
      return call;
    }

    @Override
    public String toString() {
      return getKind().toString() + ":" + getNode() + " call:" + call + " v" + getValueNumber();
    }

    public int getValueNumber() {
      return valueNumber;
    }

    @Override
    public boolean equals(Object obj) {
      if (getClass().equals(obj.getClass())) {
        ParamCaller other = (ParamCaller) obj;
        return getNode().equals(other.getNode()) && valueNumber == other.valueNumber && call.equals(other.call);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return getNode().hashCode() + 17 * valueNumber + 23 * call.hashCode();
    }

  }

  public static class ParamCallee extends ParamStatement implements ValueNumberCarrier {
    protected final int valueNumber;

    public ParamCallee(CGNode node, int valueNumber) {
      super(node);
      this.valueNumber = valueNumber;
    }

    @Override
    public Kind getKind() {
      return Kind.PARAM_CALLEE;
    }

    public int getValueNumber() {
      return valueNumber;
    }

    @Override
    public boolean equals(Object obj) {
      if (getClass().equals(obj.getClass())) {
        ParamCallee other = (ParamCallee) obj;
        return getNode().equals(other.getNode()) && valueNumber == other.valueNumber;
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return getNode().hashCode() + 97 * valueNumber;
    }

    @Override
    public String toString() {
      return getKind().toString() + ":" + getNode() + " v" + valueNumber;
    }
  }

  public static class NormalReturnCaller extends ParamStatement implements ValueNumberCarrier, CallStatementCarrier {
    private final SSAInvokeInstruction call;

    protected final int valueNumber;

    public NormalReturnCaller(CGNode node, SSAInvokeInstruction call) {
      super(node);
      this.call = call;
      this.valueNumber = call.getDef();
    }

    @Override
    public Kind getKind() {
      return Kind.NORMAL_RET_CALLER;
    }

    public SSAInvokeInstruction getCall() {
      return call;
    }

    @Override
    public String toString() {
      return getKind().toString() + ":" + getNode() + " call:" + call;
    }

    public int getValueNumber() {
      return valueNumber;
    }

    @Override
    public boolean equals(Object obj) {
      if (getClass().equals(obj.getClass())) {
        NormalReturnCaller other = (NormalReturnCaller) obj;
        return getNode().equals(other.getNode()) && valueNumber == other.valueNumber && call.equals(other.call);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return getNode().hashCode() + 117 * valueNumber + 103 * call.hashCode();
    }
  }

  public static class ExceptionalReturnCaller extends ParamStatement implements ValueNumberCarrier, CallStatementCarrier {
    private final SSAInvokeInstruction call;

    protected final int valueNumber;

    public ExceptionalReturnCaller(CGNode node, SSAInvokeInstruction call) {
      super(node);
      this.call = call;
      this.valueNumber = call.getException();
    }

    @Override
    public Kind getKind() {
      return Kind.EXC_RET_CALLER;
    }

    public SSAInvokeInstruction getCall() {
      return call;
    }

    @Override
    public String toString() {
      return getKind().toString() + ":" + getNode() + " call:" + call;
    }

    public int getValueNumber() {
      return valueNumber;
    }

    @Override
    public boolean equals(Object obj) {
      if (getClass().equals(obj.getClass())) {
        ExceptionalReturnCaller other = (ExceptionalReturnCaller) obj;
        return getNode().equals(other.getNode()) && valueNumber == other.valueNumber && call.equals(other.call);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return getNode().hashCode() + 1001 * valueNumber + 2003 * call.hashCode();
    }
  }

  public static class NormalReturnCallee extends ParamStatement {

    public NormalReturnCallee(CGNode node) {
      super(node);
    }

    @Override
    public Kind getKind() {
      return Kind.NORMAL_RET_CALLEE;
    }

    @Override
    public boolean equals(Object obj) {
      if (getClass().equals(obj.getClass())) {
        NormalReturnCallee other = (NormalReturnCallee) obj;
        return getNode().equals(other.getNode());
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return getNode().hashCode() * 89;
    }

    @Override
    public String toString() {
      return getKind().toString() + ":" + getNode();
    }
  }

  public static class ExceptionalReturnCallee extends ParamStatement {

    public ExceptionalReturnCallee(CGNode node) {
      super(node);
    }

    @Override
    public Kind getKind() {
      return Kind.EXC_RET_CALLEE;
    }

    @Override
    public boolean equals(Object obj) {
      if (getClass().equals(obj.getClass())) {
        ExceptionalReturnCallee other = (ExceptionalReturnCallee) obj;
        return getNode().equals(other.getNode());
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return getNode().hashCode() * 89;
    }

    @Override
    public String toString() {
      return getKind().toString() + ":" + getNode();
    }
  }
}
