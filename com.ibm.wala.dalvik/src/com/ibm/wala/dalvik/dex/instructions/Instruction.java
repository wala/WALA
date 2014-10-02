/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package com.ibm.wala.dalvik.dex.instructions;

import org.jf.dexlib.Code.Opcode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.dalvik.classLoader.DexIMethod;

public abstract class Instruction {

    public static class Visitor {
    	private static final Logger logger = LoggerFactory.getLogger(Instruction.Visitor.class);

        public void visitArrayLength(ArrayLength instruction) {
            logger.error("visitArrayLength not overridden");
        }

        public void visitArrayGet(ArrayGet instruction) {
            logger.error("visitArrayGet not overridden");
        }

        public void visitArrayPut(ArrayPut instruction) {
            logger.error("visitArrayPut not overridden");
        }

        public void visitArrayFill(ArrayFill instruction) {
            logger.error("visitArrayFill not overridden");
        }

        public void visitBinaryOperation(BinaryOperation instruction) {
            logger.error("visitBinaryOperation not overridden");
        }

        public void visitBinaryLiteral(BinaryLiteralOperation binaryLiteralOperation) {
            logger.error("visitBinaryLiteral not overridden");
        }

        public void visitBranch(Branch instruction) {
            logger.error("visitBranch not overridden");
        }

        public void visitCheckCast(CheckCast checkCast) {
            logger.error("visitCheckCast not overridden");
        }

        public void visitConstant(Constant instruction) {
            logger.error("visitConstant not overridden");
        }

        public void visitGetField(GetField instruction) {
            logger.error("visitGetField not overridden");
        }

        public void visitGoto(Goto inst) {
            logger.error("visitGoto not overridden");
        }

        public void visitInstanceof(InstanceOf instruction) {
            logger.error("visitInstanceof not overridden");
        }

        public void visitInvoke(Invoke instruction) {
            logger.error("visitInvoke not overridden");
        }

        public void visitMonitor(Monitor instruction) {
            logger.error("visitMonitor not overridden");
        }

        public void visitNew(New instruction) {
            logger.error("visitNew not overridden");
        }

        public void visitNewArray(NewArray newArray) {
            logger.error("visitNewArray not overridden");
        }

        public void visitNewArrayFilled(NewArrayFilled newArrayFilled) {
            logger.error("visitNewArrayFilled not overridden");
        }

        public void visitPutField(PutField instruction) {
            logger.error("visitPutField not overridden");
        }

        public void visitReturn(Return return1) {
            logger.error("visitReturn not overridden");
        }

        public void visitSwitch(Switch instruction) {
            logger.error("visitSwitch not overridden");
        }

        public void visitThrow(Throw instruction) {
            logger.error("visitThrow not overridden");
        }

        public void visitUnaryOperation(UnaryOperation instruction) {
            logger.error("visitUnaryOperation not overridden");
        }



    }

    public final int pc;
    final protected Opcode opcode;
    final protected DexIMethod method;


    public final static int[] noInstructions = new int[0];

    protected Instruction(int pc, Opcode op, DexIMethod method)
    {
        this.pc = pc;
        this.opcode = op;
        this.method = method;
    }


    /**
     * True if the instruction can continue.
     * @see com.ibm.wala.shrikeBT.IInstruction#isFallThrough()
     */
    public boolean isFallThrough() {
        return opcode.canContinue();
    }

    /**
     * True if the instruction can throw an exception
     * @see com.ibm.wala.shrikeBT.IInstruction#isPEI()
     */
    public boolean isPEI() {
        return opcode.canThrow();
    }

    /**
     * @return The DexIMethod which contains this instruction.
     */
    public DexIMethod getParentMethod(){
        return method;
    }

    /**
     * @return The opcode associated with this instruction.
     */
    public Opcode getOpcode(){
        return opcode;
    }

    public int[] getBranchTargets() {
        return noInstructions;
    }



    public abstract void visit(Visitor visitor);

}
