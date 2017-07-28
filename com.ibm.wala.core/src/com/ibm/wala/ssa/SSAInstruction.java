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
import java.util.Collections;

import com.ibm.wala.types.TypeReference;

/**
 * An instruction in SSA form.
 */
public abstract class SSAInstruction {

  public static final int NO_INDEX = -1;
  
  public final int iindex;
  
  /**
   * prevent instantiation by the outside
   */
  protected SSAInstruction(int iindex) {
    this.iindex = iindex;
  }

  /**
   * This method is meant to be used during SSA conversion for an IR that is not in SSA form. It creates a new SSAInstruction of the
   * same type as the receiver, with a combination of the receiver's uses and defs and those from the method parameters.
   * 
   * In particular, if the 'defs' parameter is null, then the new instruction has the same defs as the receiver. If 'defs' is not
   * null, it must be an array with a size equal to the number of defs that the receiver instruction has. In this case, the new
   * instruction has defs taken from the array. The uses of the new instruction work in the same way with the 'uses' parameter.
   * 
   * Note that this only applies to CAst-based IR translation, since Java bytecode-based IR generation uses a different SSA
   * construction mechanism.
   * 
   * TODO: move this into the SSAInstructionFactory
   */
  public abstract SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses);

  public abstract String toString(SymbolTable symbolTable);

  @Override
  public String toString() {
    return toString(null);
  }

  protected String getValueString(SymbolTable symbolTable, int valueNumber) {
    if (symbolTable == null) {
      return Integer.toString(valueNumber);
    } else {
      return symbolTable.getValueString(valueNumber);
    }
  }

  /**
   * Apply an IVisitor to this instruction. We invoke the appropriate IVisitor method according to the type of this instruction.
   */
  public abstract void visit(IVisitor v);

  /**
   * This interface is used by Instruction.visit to dispatch based on the instruction type.
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

    void visitLoadMetadata(SSALoadMetadataInstruction instruction);
  }

  /**
   * A base visitor implementation that does nothing.
   */
  public static abstract class Visitor implements IVisitor {
    @Override
    public void visitGoto(SSAGotoInstruction instruction) {
    }

    @Override
    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
    }

    @Override
    public void visitArrayStore(SSAArrayStoreInstruction instruction) {
    }

    @Override
    public void visitBinaryOp(SSABinaryOpInstruction instruction) {
    }

    @Override
    public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
    }

    @Override
    public void visitConversion(SSAConversionInstruction instruction) {
    }

    @Override
    public void visitComparison(SSAComparisonInstruction instruction) {
    }

    @Override
    public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
    }

    @Override
    public void visitSwitch(SSASwitchInstruction instruction) {
    }

    @Override
    public void visitReturn(SSAReturnInstruction instruction) {
    }

    @Override
    public void visitGet(SSAGetInstruction instruction) {
    }

    @Override
    public void visitPut(SSAPutInstruction instruction) {
    }

    @Override
    public void visitInvoke(SSAInvokeInstruction instruction) {
    }

    @Override
    public void visitNew(SSANewInstruction instruction) {
    }

    @Override
    public void visitArrayLength(SSAArrayLengthInstruction instruction) {
    }

    @Override
    public void visitThrow(SSAThrowInstruction instruction) {
    }

    @Override
    public void visitMonitor(SSAMonitorInstruction instruction) {
    }

    @Override
    public void visitCheckCast(SSACheckCastInstruction instruction) {
    }

    @Override
    public void visitInstanceof(SSAInstanceofInstruction instruction) {
    }

    @Override
    public void visitPhi(SSAPhiInstruction instruction) {
    }

    @Override
    public void visitPi(SSAPiInstruction instruction) {
    }

    @Override
    public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
    }

    @Override
    public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
    }
  }

  /**
   * Does this instruction define a normal value, as distinct from a set of exceptions possibly thrown by it (e.g. for invoke
   * instructions).
   * 
   * @return true if the instruction does define a proper value.
   */
  public boolean hasDef() {
    return false;
  }

  public int getDef() {
    return -1;
  }

  /**
   * Return the ith def
   * 
   * @param i number of the def, starting at 0.
   */
  public int getDef(int i) {
    return -1;
  }

  public int getNumberOfDefs() {
    return 0;
  }

  public int getNumberOfUses() {
    return 0;
  }

  /**
   * @return value number representing the jth use in this instruction. -1 means TOP (i.e., the value doesn't matter)
   */
  public int getUse(@SuppressWarnings("unused") int j) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public abstract int hashCode();

  /**
   * @return true iff this instruction may throw an exception.
   */
  public boolean isPEI() {
    return false;
  }

  /**
   * This method should never return null.
   * 
   * @return the set of exception types that an instruction might throw ... disregarding athrows and invokes.
   */
  public Collection<TypeReference> getExceptionTypes() {
    assert !isPEI();
    return Collections.emptySet();
  }

  /**
   * @return true iff this instruction may fall through to the next
   */
  public abstract boolean isFallThrough();

  /**
   * We assume these instructions are canonical and managed by a governing IR object. Be careful.
   * 
   * Depending on the caching policy (see {@link com.ibm.wala.ssa.SSACache}), the governing IR may be deleted
   * to reclaim memory and recomputed as needed. When an IR is recomputed, it also creates fresh SSAInstruction 
   * objects that will not equal old ones. Thus, do not  compare for identity SSAInstructions obtained from 
   * distinct calls that retrieve cached values (e.g. distinct CGNode.getIR() calls).
   * See <a href="https://github.com/wala/WALA/issues/6"> the github issue </a> for details.
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public final boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj != null && obj instanceof SSAInstruction)
      return this.iindex == ((SSAInstruction) obj).iindex;
    else
      return false;
  }
}
