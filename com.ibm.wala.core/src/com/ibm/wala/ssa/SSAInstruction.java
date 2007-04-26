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
package com.ibm.wala.ssa;

import java.util.Collection;

import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * An instruction in SSA form.
 * 
 * @author sfink
 * @author roca (visitor pattern copied from Shrike)
 */
public abstract class SSAInstruction implements IInstruction {

  /**
   * prevent instantiation by the outside
   */
  protected SSAInstruction() {
  }

  /**
   * TODO: document me ... what do my parameters mean? (Julian?)
   */
  public abstract SSAInstruction copyForSSA(int[] defs, int[] uses);

  /**
   * Method toString.
   * 
   * @param symbolTable
   * @return Object
   */
  public abstract String toString(SymbolTable symbolTable, ValueDecorator d);

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return toString(null, null);
  }

  protected String getValueString(SymbolTable symbolTable, ValueDecorator d, int valueNumber) {
    if (symbolTable == null) {
      return Integer.toString(valueNumber);
    } else {
      if (d == null) {
        return symbolTable.getValueString(valueNumber);
      } else {
        return d.getValueString(valueNumber);
      }
    }
  }

  /**
   * Apply an IVisitor to this instruction. We invoke the appropriate IVisitor
   * method according to the type of this instruction.
   */
  public abstract void visit(IVisitor v);

  /**
   * This interface is used by Instruction.visit to dispatch based on the
   * instruction type.
   */
  public static interface IVisitor {
    void visitGoto(SSAGotoInstruction instruction);
    void visitArrayLoad(SSAArrayLoadInstruction instruction);
    void visitArrayStore(SSAArrayStoreInstruction instruction);
    void visitBinaryOp(SSABinaryOpInstruction instruction);
    void visitUnaryOp(SSAUnaryOpInstruction instruction);
    void visitConversion(SSAConversionInstruction instruction);
    void visitComparison(SSAComparisonInstruction instruction);
    void visitConditionalBranch(SSAConditionalBranchInstruction instruction);
    void visitSwitch(SSASwitchInstruction instruction);
    void visitReturn(SSAReturnInstruction instruction);
    void visitGet(SSAGetInstruction instruction);
    void visitPut(SSAPutInstruction instruction);
    void visitInvoke(SSAInvokeInstruction instruction);
    void visitNew(SSANewInstruction instruction);
    void visitArrayLength(SSAArrayLengthInstruction instruction);
    void visitThrow(SSAThrowInstruction instruction);
    void visitMonitor(SSAMonitorInstruction instruction);
    void visitCheckCast(SSACheckCastInstruction instruction);
    void visitInstanceof(SSAInstanceofInstruction instruction);
    void visitPhi(SSAPhiInstruction instruction);
    void visitPi(SSAPiInstruction instruction);
    void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction);
    void visitLoadClass(SSALoadClassInstruction instruction);
  }

  /**
   * A base visitor implementation that does nothing.
   */
  public static abstract class Visitor implements IVisitor {
    public void visitGoto(SSAGotoInstruction instruction) {
    }

    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
    }

    public void visitArrayStore(SSAArrayStoreInstruction instruction) {
    }

    public void visitBinaryOp(SSABinaryOpInstruction instruction) {
    }

    public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
    }

    public void visitConversion(SSAConversionInstruction instruction) {
    }

    public void visitComparison(SSAComparisonInstruction instruction) {
    }

    public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
    }

    public void visitSwitch(SSASwitchInstruction instruction) {
    }

    public void visitReturn(SSAReturnInstruction instruction) {
    }

    public void visitGet(SSAGetInstruction instruction) {
    }

    public void visitPut(SSAPutInstruction instruction) {
    }

    public void visitInvoke(SSAInvokeInstruction instruction) {
    }

    public void visitNew(SSANewInstruction instruction) {
    }

    public void visitArrayLength(SSAArrayLengthInstruction instruction) {
    }

    public void visitThrow(SSAThrowInstruction instruction) {
    }

    public void visitMonitor(SSAMonitorInstruction instruction) {
    }

    public void visitCheckCast(SSACheckCastInstruction instruction) {
    }

    public void visitInstanceof(SSAInstanceofInstruction instruction) {
    }

    public void visitPhi(SSAPhiInstruction instruction) {
    }

    public void visitPi(SSAPiInstruction instruction) {
    }

    public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
    }
    public void visitLoadClass(SSALoadClassInstruction instruction) {
    }
  }

  /**
   * Does this instruction define a normal value, as distinct from a
   * set of exceptions possibly thrown by it (e.g. for invoke instructions).
   * 
   * @return true if the instruction does define a proper value.
   */
  public boolean hasDef() {
    return false;
  }

  public int getDef() {
    return -1;
  }

  public int getDef(int i) {
    return -1;
  }

  public int getNumberOfDefs() {
    return 0;
  }

  /**
   * Method getNumberOfUses.
   * 
   * @return int
   */
  public int getNumberOfUses() {
    return 0;
  }

  /**
   * Method getUse.
   * 
   * @param j
   * @return value number representing the jth use in this instruction. -1 means
   *         TOP (i.e., the value doesn't matter)
   */
  public int getUse(int j) {
    Assertions.UNREACHABLE();
    return -1;
  }

  public abstract int hashCode();

  /**
   * @return true iff this instruction may throw an exception.
   */
  public boolean isPEI() {
    return false;
  }

  /**
   * @return Colection<TypeReference> the set of exception types that an instruction might throw ...
   *         disregarding athrows and invokes.
   */
  abstract public Collection<TypeReference> getExceptionTypes();

  /**
   * @return true iff this instruction may fall through to the next
   */
  public abstract boolean isFallThrough();

  /**
   * We assume these instructions are canonical and managed by a governing IR
   * object. Be careful.
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    return this == obj;
  }
}
