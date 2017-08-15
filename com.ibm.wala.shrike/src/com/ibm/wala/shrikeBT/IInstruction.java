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
 * @author sfink
 * 
 *         Basic functionality we expect of any instruction implementation
 */
public interface IInstruction {

  /**
   * This class is used by IInstruction.visit to dispatch based on the instruction type.
   */
  @SuppressWarnings("unused")
  public static abstract class Visitor {
    public void visitConstant(ConstantInstruction instruction) {
    }

    public void visitGoto(GotoInstruction instruction) {
    }

    public void visitLocalLoad(ILoadInstruction instruction) {
    }

    public void visitLocalStore(IStoreInstruction instruction) {
    }

    public void visitArrayLoad(IArrayLoadInstruction instruction) {
    }

    public void visitArrayStore(IArrayStoreInstruction instruction) {
    }

    public void visitPop(PopInstruction instruction) {
    }

    public void visitDup(DupInstruction instruction) {
    }

    public void visitSwap(SwapInstruction instruction) {
    }

    public void visitBinaryOp(IBinaryOpInstruction instruction) {
    }

    public void visitUnaryOp(IUnaryOpInstruction instruction) {
    }

    public void visitShift(IShiftInstruction instruction) {
    }

    public void visitConversion(IConversionInstruction instruction) {
    }

    public void visitComparison(IComparisonInstruction instruction) {
    }

    public void visitConditionalBranch(IConditionalBranchInstruction instruction) {
    }

    public void visitSwitch(SwitchInstruction instruction) {
    }

    public void visitReturn(ReturnInstruction instruction) {
    }

    public void visitGet(IGetInstruction instruction) {
    }

    public void visitPut(IPutInstruction instruction) {
    }

    public void visitInvoke(IInvokeInstruction instruction) {
    }

    public void visitNew(NewInstruction instruction) {
    }

    public void visitArrayLength(ArrayLengthInstruction instruction) {
    }

    public void visitThrow(ThrowInstruction instruction) {
    }

    public void visitMonitor(MonitorInstruction instruction) {
    }

    public void visitCheckCast(ITypeTestInstruction instruction) {
    }

    public void visitInstanceof(IInstanceofInstruction instruction) {
    }

    public void visitLoadIndirect(ILoadIndirectInstruction instruction) { 
    }

    public void visitStoreIndirect(IStoreIndirectInstruction instruction) {  
    }
  }

  /**
   * @return true if the instruction can "fall through" to the following instruction
   */
  public boolean isFallThrough();

  /**
   * @return an array containing the labels this instruction can branch to (not including the following instruction if this
   *         instruction 'falls through')
   */
  public int[] getBranchTargets();

  /**
   * @return an Instruction equivalent to this one but with any branch labels updated by looking them up in the targetMap array
   */
  public IInstruction redirectTargets(int[] targetMap);

  /**
   * @return the number of values this instruction pops off the working stack
   */
  public int getPoppedCount();

  /**
   * Computes the type of data pushed onto the stack, or null if none is pushed.
   * 
   * @param poppedTypesToCheck the types of the data popped off the stack by this instruction; if poppedTypes is null, then we don't
   *          know the incoming stack types and the result of this method may be less accurate
   */
  public String getPushedType(String[] poppedTypesToCheck);

  /**
   * @return the JVM word size of the value this instruction pushes onto the stack, or 0 if this instruction doesn't push anything
   *         onto the stack.
   */
  public byte getPushedWordSize();

  /**
   * Apply a Visitor to this instruction. We invoke the appropriate Visitor method according to the type of this instruction.
   */
  public void visit(IInstruction.Visitor v);

  /**
   * Subclasses must implement toString.
   */
  @Override
  public String toString();

  /**
   * PEI == "Potentially excepting instruction"
   * 
   * @return true iff this instruction might throw an exception
   */
  boolean isPEI();

}
