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

import com.ibm.wala.dalvik.classLoader.DexIMethod;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.IOperator;

public abstract class Branch extends Instruction {

    public final int offset;
    private int label;

    protected Branch(int instLoc, int offset, Opcode opcode, DexIMethod method)
    {
        super(instLoc, opcode, method);
        this.offset = offset;
    }


    public static class UnaryBranch extends Branch
    {
        public enum CompareOp {EQZ,NEZ,LTZ,LEZ,GTZ,GEZ}
        public final int oper1;
        public final CompareOp op;

        public UnaryBranch(int instLoc, int offset, CompareOp op, int oper1, Opcode opcode, DexIMethod method)
        {
            super(instLoc,offset, opcode, method);
            this.op = op;
            this.oper1 = oper1;
        }

        @Override
        public IOperator getOperator() {
            switch(op)
            {
            case EQZ:
                return IConditionalBranchInstruction.Operator.EQ;
            case NEZ:
                return IConditionalBranchInstruction.Operator.NE;
            case LTZ:
                return IConditionalBranchInstruction.Operator.LT;
            case LEZ:
                return IConditionalBranchInstruction.Operator.LE;
            case GTZ:
                return IConditionalBranchInstruction.Operator.GT;
            case GEZ:
                return IConditionalBranchInstruction.Operator.GE;
            default:
                return null;
            }
        }
    }

    public static class BinaryBranch extends Branch
    {
        public enum CompareOp {EQ,NE,LT,LE,GT,GE}
        public final int oper1;
        public final int oper2;
        public final CompareOp op;

        public BinaryBranch(int instLoc, int offset, CompareOp op, int oper1, int oper2, Opcode opcode, DexIMethod method)
        {
            super(instLoc,offset, opcode, method);
            this.op = op;
            this.oper1 = oper1;
            this.oper2 = oper2;
        }

        @Override
        public IOperator getOperator() {
            switch(op)
            {
            case EQ:
                return IConditionalBranchInstruction.Operator.EQ;
            case NE:
                return IConditionalBranchInstruction.Operator.NE;
            case LT:
                return IConditionalBranchInstruction.Operator.LT;
            case LE:
                return IConditionalBranchInstruction.Operator.LE;
            case GT:
                return IConditionalBranchInstruction.Operator.GT;
            case GE:
                return IConditionalBranchInstruction.Operator.GE;
            default:
                return null;
            }
        }
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.visitBranch(this);
    }

    public abstract IOperator getOperator();

    @Override
    public int[] getBranchTargets() {
        this.label = method.getInstructionIndex(pc + offset);
        int[] r = { label };
        return r;
    }


}
