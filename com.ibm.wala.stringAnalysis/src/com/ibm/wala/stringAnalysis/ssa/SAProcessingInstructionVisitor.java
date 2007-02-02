/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.stringAnalysis.ssa;

import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.ssa.*;

public class SAProcessingInstructionVisitor 
  implements AstInstructionVisitor
{
    public interface Processor {
      public void onUnsupportedInstruction(SSAInstruction instruction);
      public void onSSAAbstractInvokeInstruction(SSAAbstractInvokeInstruction instruction);
      public void onSSABinaryOpInstruction(SSABinaryOpInstruction instruction);
      public void onSSAAbstractUnaryInstruction(SSAAbstractUnaryInstruction instruction);
      public void onSSANewInstruction(SSANewInstruction instruction);
      public void onSSAPhiInstruction(SSAPhiInstruction instruction);
      public void onSSAConditionalBranchInstruction(SSAConditionalBranchInstruction instruction);
      public void onSSAReturnInstruction(SSAReturnInstruction instruction);
      public void onAstLexicalRead(AstLexicalRead instruction);
      public void onAstLexicalWrite(AstLexicalWrite instruction);
      public void onSSAPutInstruction(SSAPutInstruction instruction);
      public void onSSAGetInstruction(SSAGetInstruction instruction);
    }

    protected final Processor processor;

    public SAProcessingInstructionVisitor(Processor processor) {
      this.processor = processor;
    }

    public void visitGoto(SSAGotoInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitArrayStore(SSAArrayStoreInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
        
    }

    public void visitBinaryOp(SSABinaryOpInstruction instruction) {
        processor.onSSABinaryOpInstruction(instruction);
    }

    public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
	processor.onSSAAbstractUnaryInstruction(instruction);
    }

    public void visitConversion(SSAConversionInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitComparison(SSAComparisonInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
        processor.onSSAConditionalBranchInstruction(instruction);
    }

    public void visitSwitch(SSASwitchInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitReturn(SSAReturnInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitGet(SSAGetInstruction instruction) {
        processor.onSSAGetInstruction(instruction);
    }

    public void visitPut(SSAPutInstruction instruction) {
        processor.onSSAPutInstruction(instruction);
    }

    public void visitInvoke(SSAInvokeInstruction instruction) {
        processor.onSSAAbstractInvokeInstruction(instruction);
    }

    public void visitNew(SSANewInstruction instruction) {
        processor.onSSANewInstruction(instruction);
    }

    public void visitArrayLength(SSAArrayLengthInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitThrow(SSAThrowInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitMonitor(SSAMonitorInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitCheckCast(SSACheckCastInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitInstanceof(SSAInstanceofInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitPhi(SSAPhiInstruction instruction) {
        processor.onSSAPhiInstruction(instruction);
    }

    public void visitPi(SSAPiInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitLoadClass(SSALoadClassInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitEachElementGet(EachElementGetInstruction instruction) {
        processor.onSSAAbstractUnaryInstruction(instruction);
    }

    public void visitEachElementHasNext(EachElementHasNextInstruction instruction) {
        processor.onSSAAbstractUnaryInstruction(instruction);
    }

    public void visitAstLexicalRead(AstLexicalRead instruction) {
        processor.onAstLexicalRead(instruction);
    }

    public void visitAstLexicalWrite(AstLexicalWrite instruction) {
        processor.onAstLexicalWrite(instruction);
    }

    public void visitAstGlobalRead(AstGlobalRead instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitNonExceptingThrow(NonExceptingThrowInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }

    public void visitAssert(AstAssertInstruction instruction) {
        processor.onUnsupportedInstruction(instruction);
    }
}
