/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT;

/**
 * This class represents method invocation instructions.
 */
public class InvokeInstruction extends Instruction implements IInvokeInstruction {
  protected String type;

  protected String classType;

  protected String methodName;

  InvokeInstruction(short opcode, String type, String classType, String methodName) {
    super(opcode);
    this.type = type;
    this.classType = classType;
    this.methodName = methodName;
  }

  public static InvokeInstruction make(String type, String className, String methodName, Dispatch mode) throws NullPointerException {
    if (type == null) {
      throw new NullPointerException("type must not be null");
    }
    if (className == null) {
      throw new NullPointerException("className must not be null");
    }
    if (methodName == null) {
      throw new NullPointerException("methodName must not be null");
    }
    if (mode == null) {
      throw new NullPointerException("mode must not be null");
    }
    short opcode = 0;
    switch (mode) {
    case VIRTUAL:
      opcode = OP_invokevirtual;
      break;
    case SPECIAL:
      opcode = OP_invokespecial;
      break;
    case STATIC:
      opcode = OP_invokestatic;
      break;
    case INTERFACE:
      opcode = OP_invokeinterface;
      break;
    default:
      assert false;
    }
    return new InvokeInstruction(opcode, type, className, methodName);
  }

  ConstantPoolReader getLazyConstantPool() {
    return null;
  }

  final static class Lazy extends InvokeInstruction {
    final private ConstantPoolReader cp;

    final private int index;

    Lazy(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, null, null, null);
      this.index = index;
      this.cp = cp;
    }

    @Override
    ConstantPoolReader getLazyConstantPool() {
      return cp;
    }

    int getCPIndex() {
      return index;
    }

    @Override
    public String getClassType() {
      if (classType == null) {
        classType = cp.getConstantPoolMemberClassType(index);
      }
      return classType;
    }

    @Override
    public String getMethodName() {
      if (methodName == null) {
        methodName = cp.getConstantPoolMemberName(index);
      }
      return methodName;
    }

    @Override
    public String getMethodSignature() {
      if (type == null) {
        type = cp.getConstantPoolMemberType(index);
      }
      return type;
    }
  }

  static InvokeInstruction make(ConstantPoolReader cp, int index, int mode) {
    if (mode < OP_invokevirtual || mode > OP_invokeinterface) {
      throw new IllegalArgumentException("Unknown mode: " + mode);
    }
    return new Lazy((short) mode, cp, index);
  }

  @Override
  final public boolean equals(Object o) {
    if (o instanceof InvokeInstruction) {
      InvokeInstruction i = (InvokeInstruction) o;
      return i.getMethodSignature().equals(getMethodSignature()) && i.getClassType().equals(getClassType())
          && i.getMethodName().equals(getMethodName()) && i.opcode == opcode;
    } else {
      return false;
    }
  }

  @Override
  public String getClassType() {
    return classType;
  }

  @Override
  public String getMethodName() {
    return methodName;
  }

  @Override
  public String getMethodSignature() {
    return type;
  }

  final public int getInvocationMode() {
    return opcode;
  }

  final public String getInvocationModeString() {
    switch (opcode) {
    case Constants.OP_invokestatic:
      return "STATIC";
    case Constants.OP_invokeinterface:
      return "INTERFACE";
    case Constants.OP_invokespecial:
      return "SPECIAL";
    case Constants.OP_invokevirtual:
      return "VIRTUAL";
    default:
      throw new Error("Unknown mode: " + opcode);
    }
  }

  @Override
  final public int hashCode() {
    return getMethodSignature().hashCode() + 9011 * getClassType().hashCode() + 317 * getMethodName().hashCode() + opcode * 3188;
  }

  @Override
  final public int getPoppedCount() {
    return (opcode == Constants.OP_invokestatic ? 0 : 1) + Util.getParamsCount(getMethodSignature());
  }

  @Override
  final public String getPushedType(String[] types) {
    String t = Util.getReturnType(getMethodSignature());
    if (t.equals(Constants.TYPE_void)) {
      return null;
    } else {
      return t;
    }
  }

  @Override
  final public byte getPushedWordSize() {
    String t = getMethodSignature();
    int index = t.lastIndexOf(')');
    return Util.getWordSize(t, index + 1);
  }

  @Override
  final public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitInvoke(this);
  }

  @Override
  final public String toString() {
    return "Invoke(" + getInvocationModeString() + "," + getClassType() + "," + getMethodName() + "," + getMethodSignature() + ")";
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  @Override
  public Dispatch getInvocationCode() {
    switch (opcode) {
    case Constants.OP_invokestatic:
      return Dispatch.STATIC;
    case Constants.OP_invokeinterface:
      return Dispatch.INTERFACE;
    case Constants.OP_invokespecial:
      return Dispatch.SPECIAL;
    case Constants.OP_invokevirtual:
      return Dispatch.VIRTUAL;
    default:
      throw new Error("Unknown mode: " + opcode);
    }
  }
}
