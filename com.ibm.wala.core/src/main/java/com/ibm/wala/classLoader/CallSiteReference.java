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
package com.ibm.wala.classLoader;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.shrike.shrikeBT.BytecodeConstants;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * Simple object that represents a static call site (ie., an invoke instruction in the bytecode)
 *
 * <p>Note that the identity of a call site reference depends on two things: the program counter,
 * and the containing IR. Thus, it suffices to define equals() and hashCode() from {@link
 * ProgramCounter}, since this class does not maintain a pointer to the containing {@link IR} (or
 * {@link CGNode}) anyway. If using a hashtable of CallSiteReference from different IRs, you
 * probably want to use a wrapper which also holds a pointer to the governing CGNode.
 */
public abstract class CallSiteReference extends ProgramCounter
    implements BytecodeConstants, ContextItem {

  private final MethodReference declaredTarget;

  /**
   * @param programCounter Index into bytecode describing this instruction
   * @param declaredTarget The method target as declared at the call site
   */
  protected CallSiteReference(int programCounter, MethodReference declaredTarget) {
    super(programCounter);
    this.declaredTarget = declaredTarget;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((declaredTarget == null) ? 0 : declaredTarget.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    CallSiteReference other = (CallSiteReference) obj;
    if (declaredTarget == null) {
      if (other.declaredTarget != null) return false;
    } else if (!declaredTarget.equals(other.declaredTarget)) return false;
    return true;
  }

  // the following atrocities are needed to save a word of space by
  // declaring these classes static, so they don't keep a pointer
  // to the enclosing environment
  // Java makes you type!
  static class StaticCall extends CallSiteReference {
    StaticCall(int programCounter, MethodReference declaredTarget) {
      super(programCounter, declaredTarget);
    }

    @Override
    public IInvokeInstruction.IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.STATIC;
    }
  }

  static class SpecialCall extends CallSiteReference {
    SpecialCall(int programCounter, MethodReference declaredTarget) {
      super(programCounter, declaredTarget);
    }

    @Override
    public IInvokeInstruction.IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.SPECIAL;
    }
  }

  static class VirtualCall extends CallSiteReference {
    VirtualCall(int programCounter, MethodReference declaredTarget) {
      super(programCounter, declaredTarget);
    }

    @Override
    public IInvokeInstruction.IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.VIRTUAL;
    }
  }

  static class InterfaceCall extends CallSiteReference {
    InterfaceCall(int programCounter, MethodReference declaredTarget) {
      super(programCounter, declaredTarget);
    }

    @Override
    public IInvokeInstruction.IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.INTERFACE;
    }
  }

  /**
   * This factory method plays a little game to avoid storing the invocation code in the object;
   * this saves a byte (probably actually a whole word) in each created object.
   *
   * <p>TODO: Consider canonicalization?
   */
  public static CallSiteReference make(
      int programCounter,
      MethodReference declaredTarget,
      IInvokeInstruction.IDispatch invocationCode) {

    if (invocationCode == IInvokeInstruction.Dispatch.SPECIAL)
      return new SpecialCall(programCounter, declaredTarget);
    if (invocationCode == IInvokeInstruction.Dispatch.VIRTUAL)
      return new VirtualCall(programCounter, declaredTarget);
    if (invocationCode == IInvokeInstruction.Dispatch.INTERFACE)
      return new InterfaceCall(programCounter, declaredTarget);
    if (invocationCode == IInvokeInstruction.Dispatch.STATIC)
      return new StaticCall(programCounter, declaredTarget);

    throw new IllegalArgumentException("unsupported code: " + invocationCode);
  }

  /**
   * Return the Method that this call site calls. This represents the method declared in the invoke
   * instruction only.
   */
  public MethodReference getDeclaredTarget() {
    return declaredTarget;
  }

  /** Return one of INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL, or INVOKEINTERFACE */
  public abstract IInvokeInstruction.IDispatch getInvocationCode();

  @Override
  public String toString() {
    return "invoke"
        + getInvocationString(getInvocationCode())
        + ' '
        + declaredTarget
        + '@'
        + getProgramCounter();
  }

  protected String getInvocationString(IInvokeInstruction.IDispatch invocationCode) {
    if (invocationCode == IInvokeInstruction.Dispatch.STATIC) return "static";
    if (invocationCode == IInvokeInstruction.Dispatch.SPECIAL) return "special";
    if (invocationCode == IInvokeInstruction.Dispatch.VIRTUAL) return "virtual";
    if (invocationCode == IInvokeInstruction.Dispatch.INTERFACE) return "interface";

    Assertions.UNREACHABLE();
    return null;
  }

  public String getInvocationString() {
    return getInvocationString(getInvocationCode());
  }

  /** Is this an invokeinterface call site? */
  public final boolean isInterface() {
    return (getInvocationCode() == IInvokeInstruction.Dispatch.INTERFACE);
  }

  /** Is this an invokevirtual call site? */
  public final boolean isVirtual() {
    return (getInvocationCode() == IInvokeInstruction.Dispatch.VIRTUAL);
  }

  /** Is this an invokespecial call site? */
  public final boolean isSpecial() {
    return (getInvocationCode() == IInvokeInstruction.Dispatch.SPECIAL);
  }

  /** Is this an invokestatic call site? */
  public boolean isStatic() {
    return (getInvocationCode() == IInvokeInstruction.Dispatch.STATIC);
  }

  public boolean isFixed() {
    return isStatic() || isSpecial();
  }

  public boolean isDispatch() {
    return isVirtual() || isInterface();
  }
}
